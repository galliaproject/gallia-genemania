package galliaexample.genemania

import aptus._ // for timing

// ===========================================================================
object GeneManiaDriver {

  val Parent =
    //"http://genemania.org/data/current/Homo_sapiens"
    "/data/genemania/weights" // local copies to avoid hammering the server

  // ---------------------------------------------------------------------------
  val Compression =
    //""  // not compression on server
    ".gz" // compressed locally to save space

  // ===========================================================================
  @annotation.nowarn def main(args: Array[String]): Unit = {
   /*
    //gallia.Hacks.iteratorParGroupSize.setValue(50) // number depends a lot on #cpus/RAM
      gallia.Hacks.disableRuntimeChecks.setToTrue()
      gallia.Hacks.loseOrderOnGrouping .setToTrue()
    */

    // ---------------------------------------------------------------------------
    ().time.seconds {
      new GeneMania(
          inputDirPath = Parent, inputCompression = ".gz", maxFiles = Some(3),
          outputPath = "/tmp/genemania.jsonl.gz")      
      .apply()
    }
  }

}

// ===========================================================================

