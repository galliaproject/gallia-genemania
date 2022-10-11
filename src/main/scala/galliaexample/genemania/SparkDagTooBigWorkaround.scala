package galliaexample.genemania

import scala.util.chaining._
import aptus._
import gallia._

// ===========================================================================
/** 
 * explained in https://stackoverflow.com/a/42451307/4228079
 *
 * this is more an issue with Spark, not something Gallia can really help with  
 */
object SparkDagTooBigWorkaround {

  /** the problem occurs past this threshold (> 175 "weights" files) */
  private val Threshold = 175

  // ---------------------------------------------------------------------------
  implicit class Implicits(fileNames: Seq[FileName]) {
    def mapWithCheckpointingGroups(maxOpt: Option[Int])(checkpointingHook: HeadS => HeadS)(f: FileName => HeadS): Seq[HeadS] =
      SparkDagTooBigWorkaround.mapWithCheckpointing(fileNames, maxOpt)(checkpointingHook)(f)
  }
  
  // ===========================================================================
  private def mapWithCheckpointing(fileNames: Seq[FileName], maxOpt: Option[Int])(checkpointingHook: HeadS => HeadS)(f: FileName => HeadS): Seq[HeadS] =
      groupSizeOpt(maxOpt.getOrElse(fileNames.size))
         match {
          case None    => fileNames.map(f)
          case Some(n) => grouping(fileNames, n)(checkpointingHook)(f) }
    
    // ---------------------------------------------------------------------------
    private def grouping(fileNames: Seq[FileName], groupSize: Int)(checkpointingHook: HeadS => HeadS)(f: FileName => HeadS): Seq[HeadS] =
        fileNames
          .grouped(groupSize)
          .map(processGroup(checkpointingHook)(f)) 
          .toList

      // ---------------------------------------------------------------------------
      private def processGroup(checkpointingHook: HeadS => HeadS)(f: FileName => HeadS)(groupedFileNames: Seq[FileName]): HeadS =
        groupedFileNames
          .map(f)
          .reduceLeft(_ union _)
          .pipe(checkpointingHook) // checkpointing not in Gallia yet, see https://github.com/galliaproject/gallia-docs/blob/master/tasks.md#t210121160956
    
  // ===========================================================================
  private def groupSizeOpt(max: Int): Option[Int] =
      if (max < Threshold) None
      else                 Some(computeGroupSizeValue(max, Threshold))
    
    // ---------------------------------------------------------------------------
    // eg:
    //  if max=200 and threshold = 175, then: 200 / ceil(200/175) = ceil(200 / 2) = 100
    //  if max=640 and threshold = 175, then: 640 / ceil(640/175) = ceil(640 / 4) = 160
    private def computeGroupSizeValue(max: Int, Max: Int): Int =
      (max.toDouble / (max.toDouble / Max).ceil.toInt).ceil.toInt // to keep the group balanced
      
  
}
// ===========================================================================

