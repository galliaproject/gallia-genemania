// gallia-genemania
// TODO: t210309100048 - relies on symlink to gallia-core's project/*.scala files; no (reasonnable) sbt way?

// ===========================================================================
lazy val root = (project in file("."))
  .settings(
    name               := "gallia-genemania",
    version            := "0.1.0",
    scalaVersion       := GalliaScalaVersions.supported.head,
    crossScalaVersions := GalliaScalaVersions.supported)
  .dependsOn(RootProject(file("../gallia-core")))

// ===========================================================================
// TODO: more + inherit from core
scalacOptions in Compile ++=
  Seq(
    "-encoding", "UTF-8",
    "-Ywarn-value-discard") ++ 
  (scalaBinaryVersion.value match {
    case "2.13" => Seq("-Ywarn-unused:imports")
    case _      => Seq("-Ywarn-unused-import" ) })

// ===========================================================================
