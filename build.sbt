import Dependencies._

ThisBuild / scalaVersion := "2.13.18"

ThisBuild / scalacOptions += "-deprecation"

lazy val root = (project in file("."))
  .settings(
    name := "manage-help-content-publisher",
    assembly / assemblyJarName := s"${name.value}.jar",
    assembly / assemblyOutputPath := file(s"${(assembly/assemblyJarName).value}"),
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

// Force patched Netty version to address CVE-2026-33871 (HTTP/2 CONTINUATION frame flood DoS)
ThisBuild / dependencyOverrides ++= Seq(
  "io.netty" % "netty-buffer"       % nettyVersion,
  "io.netty" % "netty-codec"        % nettyVersion,
  "io.netty" % "netty-codec-http"   % nettyVersion,
  "io.netty" % "netty-codec-http2"  % nettyVersion,
  "io.netty" % "netty-common"       % nettyVersion,
  "io.netty" % "netty-handler"      % nettyVersion,
  "io.netty" % "netty-resolver"     % nettyVersion,
  "io.netty" % "netty-transport"    % nettyVersion,
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
