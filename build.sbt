import Dependencies._
import com.gu.riffraff.artifact.BuildInfo

ThisBuild / scalaVersion := "2.13.8"

ThisBuild / scalacOptions += "-deprecation"

val buildInfo = Seq(
  buildInfoPackage := "build",
  buildInfoKeys ++= {
    val buildInfo = BuildInfo(baseDirectory.value)
    Seq[BuildInfoKey](
      "buildNumber" -> buildInfo.buildIdentifier
    )
  }
)

lazy val root = (project in file("."))
  .enablePlugins(BuildInfoPlugin, RiffRaffArtifact)
  .settings(
    name := "manage-help-content-publisher",
    assembly / assemblyJarName := s"${name.value}.jar",
    assembly / assemblyOutputPath := file(s"${(assembly/assemblyJarName).value}"),
    riffRaffPackageType := assembly.value,
    riffRaffUploadArtifactBucket := Option("riffraff-artifact"),
    riffRaffUploadManifestBucket := Option("riffraff-builds"),
    riffRaffManifestProjectName := s"${name.value}",
    riffRaffArtifactResources += (file("cfn.yaml"), "cfn/cfn.yaml"),
    buildInfo,
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
      slf4jNop % Runtime,
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

assembly / assemblyMergeStrategy := {
  case "module-info.class"                                      => MergeStrategy.discard
  case PathList("META-INF", "versions", _, "module-info.class") => MergeStrategy.discard
  case PathList("META-INF", "io.netty.versions.properties")     => MergeStrategy.discard
  case x =>
    val oldStrategy = (assembly / assemblyMergeStrategy).value
    oldStrategy(x)
}
