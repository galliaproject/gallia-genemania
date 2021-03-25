package galliaexample.genemania

import aptus._ // for timing

// ===========================================================================
object GeneManiaDriver {

  val Parent =
    //"http://genemania.org/data/current/Homo_sapiens"
    "/data/genemania/individuals" // local copies to avoid hammering the server

  // ---------------------------------------------------------------------------
  val Compression =
    //""  // not compression on server
    ".gz" // compressed locally to save space

  // ===========================================================================
  @annotation.nowarn def main(args: Array[String]): Unit = {
   /*
    //Hacks.IteratorParGroupSize = Some(50) // number depends a lot on #cpus/RAM
      Hacks.DisableRuntimeChecks = true
      Hacks.LoseOrderOnGrouping  = true
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

