package galliaexample.genemania

import scala.util.chaining._ // for .pipe
import aptus._ // for timing
import gallia._

// ===========================================================================
object GeneMania {

  val Parent =
    //"http://genemania.org/data/current/Homo_sapiens"
    "/data/genemania/individuals" // local copies to avoid hammering the server

  // ---------------------------------------------------------------------------
  val Compression =
    //""  // not compression on server
    ".gz" // compressed locally to save space

  // ---------------------------------------------------------------------------
  /*
    excerpt:
      File_Name                              Network_Group_Name  Network_Name             Source  Pubmed_ID
      Predicted.I2D-BIND-Fly2Human.txt       Predicted           I2D-BIND-Fly2Human       I2D     10871269
      Predicted.I2D-BIND-Mouse2Human.txt     Predicted           I2D-BIND-Mouse2Human     I2D     10871269
      Predicted.I2D-BIND-Rat2Human.txt       Predicted           I2D-BIND-Rat2Human       I2D     10871269
      ...
  */
  val Networks = s"${Parent}/networks.txt${Compression}"

  // ===========================================================================
  def main(args: Array[String]): Unit = {

   /*
    //Hacks.IteratorParGroupSize = Some(50) // number depends a lot on #cpus/RAM
      Hacks.DisableRuntimeChecks = true
      Hacks.LoseOrderOnGrouping  = true
    */

    // ---------------------------------------------------------------------------
    ().time.seconds {
      apply(maxOpt = Some(1000 /* out of ~382M */)) // to test a smaller subset
    //apply()
    }

    ()
  }

  // ---------------------------------------------------------------------------
  def apply(maxOpt: Option[Int] = None) = {
    union()
        .take(maxOpt) // out of ~382M
        .logProgress(/* every */ 100000 /* row */,   "incoming")
          .pipe(restructure)
        .logProgress(/* every */    100 /* genes */, "outgoing")
      .write("/tmp/genemania.jsonl.gz")
  }

  // ===========================================================================
  def union(): HeadS =
    networks()
      .forceStrings("File_Name")
      .filterNot(_ == "Co-expression.Honda-Kaneko-2010.txt") // empty (not even a header)
      .map { fileName =>
        weights(fileName)
          .union {
        // confirmed all interactions are symetrical (see https://groups.google.com/g/genemania-discuss/c/Go4oXNHEhoQ)
        weights(fileName).swapEntries("Gene_A", "Gene_B") } }
      .reduceLeft(_ union _)

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
      s"${Parent}/${fileName}${Compression}"
        .stream(_.tsv.iteratorMode.schema("Gene_A".string, "Gene_B".string, "Weight".double))
          .add("File_Name" -> fileName) // will be join key

  // ===========================================================================
  def restructure(union: HeadS): HeadS =
    union
       .rename(
             "Gene_A" ~> _id, // they all seem to use ensembl, for humans at least
             "Gene_B" ~> "target",
             "Weight" ~> "weight")

       .innerJoin(networks()) // will use hash join since networks() is in-memory
         .remove("File_Name") // not needed after the join (redundant)

       .groupBy(_id).as("interactions") // will leverage GNU sort since .iteratorMode (see https://github.com/galliaproject/gallia-core#poor-mans-scaling-spilling)

       .transformObjects("interactions").using {
         _ .nest("network", "source", "pubmed", "weight").under("context")
           .group("context").by("interaction", "target")
           .transformObjects("context").using {
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
  def networks(): HeadS =
    Networks
      .stream(_.tsv)
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

