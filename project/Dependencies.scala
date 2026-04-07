import sbt._

object Dependencies {
  val circeVersion = "0.14.15"
  val upickleVersion = "4.4.3"
  // CVE-2026-33870: HTTP Request Smuggling via Chunked Extension Quoted-String Parsing
  val nettyVersion = "4.1.132.Final"

  lazy val http = "org.scalaj" %% "scalaj-http" % "2.4.2"
  lazy val ujson = "com.lihaoyi" %% "ujson" % upickleVersion
  lazy val upickle = "com.lihaoyi" %% "upickle" % upickleVersion
  lazy val circeCore = "io.circe" %% "circe-core" % circeVersion
  lazy val circeGeneric = "io.circe" %% "circe-generic" % circeVersion
  lazy val circeParser = "io.circe" %% "circe-parser" % circeVersion
  lazy val jsoup = "org.jsoup" % "jsoup" % "1.22.1"
  lazy val zip = "org.zeroturnaround" % "zt-zip" % "1.15"
  lazy val utest = "com.lihaoyi" %% "utest" % "0.9.5"
  lazy val awsLambda = "com.amazonaws" % "aws-lambda-java-core" % "1.4.0"
  lazy val awsEvents = "com.amazonaws" % "aws-lambda-java-events" % "3.16.1"
  lazy val s3 = "software.amazon.awssdk" % "s3" % "2.42.15"
  lazy val slf4jNop = "org.slf4j" % "slf4j-nop" % "2.0.17"

  lazy val nettyOverrides: Seq[ModuleID] = Seq(
    "io.netty" % "netty-codec-http"                    % nettyVersion,
    "io.netty" % "netty-codec-http2"                   % nettyVersion,
    "io.netty" % "netty-codec"                         % nettyVersion,
    "io.netty" % "netty-transport"                     % nettyVersion,
    "io.netty" % "netty-common"                        % nettyVersion,
    "io.netty" % "netty-buffer"                        % nettyVersion,
    "io.netty" % "netty-handler"                       % nettyVersion,
    "io.netty" % "netty-resolver"                      % nettyVersion,
    "io.netty" % "netty-transport-classes-epoll"       % nettyVersion,
    "io.netty" % "netty-transport-native-unix-common"  % nettyVersion,
  )
}
