import Dependencies._

ThisBuild / scalaVersion := "2.13.4"

lazy val root = (project in file("."))
  .settings(
    name := "manage-help-content-publisher",
    libraryDependencies ++= Seq(
      http,
      upickle,
      circeCore,
      circeGeneric,
      circeParser,
      jsoup
    )
  )
