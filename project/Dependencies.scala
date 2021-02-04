import sbt._

object Dependencies {
  val circeVersion = "0.13.0"

  lazy val http = "org.scalaj" %% "scalaj-http" % "2.4.2"
  lazy val ujson = "com.lihaoyi" %% "ujson" % "1.2.3"
  lazy val circeCore = "io.circe" %% "circe-core" % circeVersion
  lazy val circeGeneric = "io.circe" %% "circe-generic" % circeVersion
  lazy val circeParser = "io.circe" %% "circe-parser" % circeVersion
  lazy val jsoup = "org.jsoup" % "jsoup" % "1.13.1"
  lazy val zip = "org.zeroturnaround" % "zt-zip" % "1.14"
}
