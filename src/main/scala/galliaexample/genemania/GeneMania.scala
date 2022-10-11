package galliaexample.genemania

import scala.util.chaining._
import aptus._
import gallia._

// ===========================================================================
class GeneMania(
    inputDirPath: String, inputCompression: String, maxFiles: Option[Int] = None /* None is all */,
    outputPath  : String) {
  import SparkDagTooBigWorkaround.Implicits

  // ===========================================================================
  // just a trick to simplify reusing code in the spark-version, could all be inlined for non-spark run

  var weightInputReader : String => HeadS = path => path.stream(_.tsv.iteratorMode.schema("Gene_A".string, "Gene_B".string, "Weight".double))
  var networkInputReader: String => HeadS = path => path.stream(_.tsv             .schema("File_Name".string, "Network_Group_Name".string, "Network_Name".string, "Source".string, "Pubmed_ID".string))  
  
  var outputWriter: String => HeadS => Unit = path => x => { x.write(path); () }
  
  var checkpointingHook: HeadS => HeadS = identity // don't checkpoint for non-spark run
  var coalescingHook   : HeadS => HeadS = identity // don't coalesce   for non-spark run  

  // ===========================================================================
  def apply() = {
    union()
        .logProgress(/* every */ 100000 /* row */,   "incoming")
          .pipe(restructure)
        .logProgress(/* every */    100 /* genes */, "outgoing")
      .pipe(coalescingHook)
      .pipe(outputWriter(outputPath))
  }

  // ===========================================================================
  def union(): HeadS =
    fileNames()
      .mapWithCheckpointingGroups(maxFiles)(checkpointingHook) { fileName => // vs a simple ".map { fileName =>" if not using spark
        weights(fileName)
          .union {
        // confirmed all interactions are symetrical (see https://groups.google.com/g/genemania-discuss/c/Go4oXNHEhoQ)
        weights(fileName).swapEntries("Gene_A", "Gene_B") } }
      .reduceLeft(_ union _)

    // ---------------------------------------------------------------------------
    def fileNames(): Seq[FileName] =
        networks()
          .forceStrings("File_Name")
          .filterNot(_ == "Co-expression.Honda-Kaneko-2010.txt") // empty (not even a header)
          .pipeOpt(maxFiles)(n => _.take(n))
          .sorted

    // ---------------------------------------------------------------------------
    /*
      excerpt:
        Gene_A           Gene_B           Weight
        ENSG00000000457  ENSG00000000460  1.2E-2
        ENSG00000001629  ENSG00000001631  1.8E-2
        ENSG00000000938  ENSG00000002834  3.7E-3
        ...
    */
    def weights(fileName: String): HeadS =
      s"${inputDirPath}/${fileName}${inputCompression}"
        .pipe(weightInputReader)
          .add("File_Name" -> fileName) // will be join key

  // ===========================================================================
  def restructure(union: HeadS): HeadS =
    union
       .rename(
             "Gene_A" ~> _id, // they all seem to use ensembl, for humans at least
             "Gene_B" ~> "target",
             "Weight" ~> "weight")

       .innerJoin(networks().toViewBased) // will use hash join since right-hand side not distributed (view-based); alse see t210322111234
         .remove("File_Name") // not needed after the join (redundant)

       .groupBy(_id).as("interactions") // will leverage GNU sort since .iteratorMode (see https://github.com/galliaproject/gallia-core#poor-mans-scaling-spilling)

       .transformAllEntities("interactions").using {
         _ .nest("network", "source", "pubmed", "weight").under("context")
           .group("context").by("interaction", "target")
           .transformAllEntities("context").using {
               // eg for ENSG00000006451 -> predicted -> ENSG00000116903 (0.71, then 0.12)
               _.sortByDescending("weight") }
           .nest("target", "context").under(_tmp)
           .group(_tmp).by    ("interaction")
           .pivot(_tmp).column("interaction")
             .asNewKeys( // MUST provide until https://github.com/galliaproject/gallia-docs/blob/master/tasks.md#t210110094829 addressed
                 "predicted"             ,
                 "pathway"               ,
                 "co_localization"       ,
                "genetic_interactions"  ,
                "physical_interactions" ,
                "shared_protein_domains") }
         .unnestAllFrom("interactions")

  // ===========================================================================
  /*
    excerpt:
      File_Name                              Network_Group_Name  Network_Name             Source  Pubmed_ID
      Predicted.I2D-BIND-Fly2Human.txt       Predicted           I2D-BIND-Fly2Human       I2D     10871269
      Predicted.I2D-BIND-Mouse2Human.txt     Predicted           I2D-BIND-Mouse2Human     I2D     10871269
      Predicted.I2D-BIND-Rat2Human.txt       Predicted           I2D-BIND-Rat2Human       I2D     10871269
      ...
  */   
  def networks(): HeadS =
    s"${inputDirPath}/networks.txt${inputCompression}"
      .pipe(networkInputReader)
        .rename(
          "Network_Group_Name" ~> "interaction",
          "Network_Name"       ~> "network",
          "Source"             ~> "source",
          "Pubmed_ID"          ~> "pubmed")
        .convert         ("pubmed").toStr // integer-like but not intended as such
        .removeIfValueFor("pubmed").is("0")
        .transformString("interaction").using { // eg "Co-localization" --> "co_localization"
          _.replace(" ", "_").replace("-", "_").toLowerCase }

}

// ===========================================================================

