import Dependencies._

ThisBuild / scalaVersion := "2.13.4"

lazy val root = (project in file("."))
  .settings(
    name := "manage-help-content-publisher",
    libraryDependencies ++= Seq(
      http,
      circeCore,
      circeGeneric,
      circeParser,
      jsoup
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
