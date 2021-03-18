package managehelpcontentpublisher

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.{APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.regions.Region.EU_WEST_1
import software.amazon.awssdk.services.s3.S3Client
import upickle.default._

import java.io.File
import scala.io.Source
import scala.sys.env
import scala.util.Try

object Handler {

  def handleRequest(event: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent = {

    val logger = context.getLogger
    def logInfo(message: String): Unit = logger.log(s"INFO: $message")
    def logError(message: String): Unit = logger.log(s"ERROR: $message")

    logInfo(s"Using config: $config")
    logInfo(s"Input: ${event.getBody}")
    val response = result(event.getBody) match {
      case Left(e) =>
        logError(e.reason)
        new APIGatewayProxyResponseEvent().withStatusCode(500).withBody(e.reason)
      case Right(results) =>
        results.foreach(result => logInfo(s"Wrote to ${result.path}: ${result.content}"))
        new APIGatewayProxyResponseEvent().withStatusCode(204)
    }
    logInfo(s"Response: ${response.toString}")
    response
  }

  def main(args: Array[String]): Unit = {
    val inFile = Source.fromFile(new File(args(0)))
    val input = inFile.mkString
    inFile.close()
    result(input) match {
      case Left(e) => println(s"Failed: ${e.reason}")
      case Right(results) =>
        println(s"Success!")
        results.foreach(result => println(s"Wrote to ${result.path}: ${result.content}"))
    }
  }

  private case class AwsConfig(region: Region, bucketName: String, articlesFolder: String, topicsFolder: String)
  private case class Config(stage: String, awsConfig: AwsConfig)

  private val config = {
    val stage = env.getOrElse("stage", "DEV")
    Config(
      stage,
      AwsConfig(
        region = EU_WEST_1,
        bucketName = "manage-help-content",
        articlesFolder = s"$stage/articles",
        topicsFolder = s"$stage/topics"
      )
    )
  }

  private case class PutResult(path: String, content: String)

  private def result(jsonString: String): Either[Failure, Seq[PutResult]] = {

    def readInput(jsonString: String): Either[Failure, InputModel] =
      Try(read[InputModel](jsonString)).toEither.left.map(e => Failure(s"Failed to read input: ${e.getMessage}"))

    def writeArticle(article: Article): Either[Failure, String] =
      Try(write(article)).toEither.left.map(e => Failure(s"Failed to write article: ${e.getMessage}"))

    val s3 = S3Client.builder().region(config.awsConfig.region).build()

    val putArticleInS3 =
      S3.put(s3, bucketName = config.awsConfig.bucketName, folder = config.awsConfig.articlesFolder) _

    for {
      input <- readInput(jsonString)
      article = Article.fromInput(input.article)
      json <- writeArticle(article)
      path = s"${article.path}.json"
      _ <- putArticleInS3(path, json)
    } yield Seq(PutResult(path, json))
  }
}
