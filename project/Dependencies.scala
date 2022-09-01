import sbt._

object Dependencies {
  val circeVersion = "0.14.1"
  val upickleVersion = "1.5.0"

  lazy val http = "org.scalaj" %% "scalaj-http" % "2.4.2"
  lazy val ujson = "com.lihaoyi" %% "ujson" % upickleVersion
  lazy val upickle = "com.lihaoyi" %% "upickle" % upickleVersion
  lazy val circeCore = "io.circe" %% "circe-core" % circeVersion
  lazy val circeGeneric = "io.circe" %% "circe-generic" % circeVersion
  lazy val circeParser = "io.circe" %% "circe-parser" % circeVersion
  lazy val jsoup = "org.jsoup" % "jsoup" % "1.14.3"
  lazy val zip = "org.zeroturnaround" % "zt-zip" % "1.14"
  lazy val utest = "com.lihaoyi" %% "utest" % "0.7.11"
  lazy val awsLambda = "com.amazonaws" % "aws-lambda-java-core" % "1.2.1"
  lazy val awsEvents = "com.amazonaws" % "aws-lambda-java-events" % "3.11.0"
  lazy val s3 = "software.amazon.awssdk" % "s3" % "2.17.265"
  lazy val slf4jNop = "org.slf4j" % "slf4j-nop" % "2.0.0-alpha6"
}
