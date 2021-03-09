// gallia-genemania; TODO: t210309100048 - relies on symlink to gallia-core's project/*.scala files; no (reasonnable) sbt way? windows users have to copy them instead?

lazy val root = (project in file("."))
  .settings(
    name    := "gallia-genemania",
    version := "0.1.0")
  .settings(GalliaCommonSettings.mainSettings:_*)
  .dependsOn(RootProject(file("../gallia-core")))

