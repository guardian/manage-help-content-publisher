import Dependencies._

ThisBuild / scalaVersion := "2.13.5"

lazy val root = (project in file("."))
  .enablePlugins(RiffRaffArtifact)
  .settings(
    name := "manage-help-content-publisher",
    riffRaffPackageType := (baseDirectory.value / "cfn"),
    riffRaffUploadArtifactBucket := Option("riffraff-artifact"),
    riffRaffUploadManifestBucket := Option("riffraff-builds"),
    riffRaffManifestProjectName := "manage-help-content-publisher",
    riffRaffArtifactResources += (file("cfn.yaml"), "cfn/cfn.yaml"),
    libraryDependencies ++= Seq(
      http,
      circeCore,
      circeGeneric,
      circeParser,
      jsoup,
      ujson,
      upickle,
      awsLambda,
      awsEvents,
      s3,
      utest % Test
    )
  )

// Sub-project to import content into SF Knowledge
lazy val legacyContentImport = (project in file("legacy-content-import"))
  .settings(
    name := "legacy-content-import",
    libraryDependencies ++= Seq(
      http,
      ujson,
      zip
    )
  )

testFrameworks += new TestFramework("utest.runner.Framework")

assemblyMergeStrategy in assembly := {
  case "module-info.class"                                  => MergeStrategy.discard
  case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}
