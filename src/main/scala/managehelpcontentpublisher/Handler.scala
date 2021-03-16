package managehelpcontentpublisher

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.{APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent}
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.regions.Region.EU_WEST_1
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest

import scala.util.Try
import upickle.default._

object Handler {

  def handleRequest(event: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent = {

    val logger = context.getLogger
    def logInfo(message: String): Unit = logger.log(s"INFO: $message")
    def logError(message: String): Unit = logger.log(s"ERROR: $message")

    logInfo(s"Input: ${event.getBody}")
    val response = result(event.getBody) match {
      case Left(e) =>
        logError(e.reason)
        new APIGatewayProxyResponseEvent().withStatusCode(500).withBody(e.reason)
      case Right(obj) =>
        logInfo(s"Generated: ${obj.render(indent = 2)}")
        new APIGatewayProxyResponseEvent().withStatusCode(204)
    }
    logInfo(s"Response: ${response.toString}")
    response
  }

  def main(args: Array[String]): Unit =
    result(args(0)) match {
      case Left(e)    => println(s"Failed: ${e.reason}")
      case Right(obj) => println(s"Success!: ${obj.render(indent = 2)}")
    }

  private case class AwsConfig(region: Region, bucketName: String, articlesFolder: String, topicsFolder: String)
  private case class Config(stage: String, awsConfig: AwsConfig)

  private val config = {
    val stage = "DEV"
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

  private val storeArticleInS3 = storeInS3(
    s3 = S3Client
      .builder()
      .region(config.awsConfig.region)
      .build(),
    bucketName = config.awsConfig.bucketName,
    folder = config.awsConfig.articlesFolder
  ) _

  private def result(articleJsonString: String) = for {
    article <- Try(read[Article](articleJsonString)).toEither.left.map(e =>
      Failure(s"Failed to read article from input: ${e.getMessage}")
    )
    json <- Right(toJson(article))
    _ <- storeArticleInS3(s"${article.path}.json", json)
  } yield json

  private def toJson(article: Article): ujson.Obj =
    ujson.Obj(
      "title" -> article.title,
      "body" -> HtmlToJson(article.body),
      "topics" -> article.topics.map(topic => ujson.Obj("path" -> topic.path, "title" -> topic.name))
    )

  private def storeInS3(s3: S3Client, bucketName: String, folder: String)(
      fileName: String,
      content: ujson.Obj
  ): Either[Failure, Unit] =
    Try(
      s3.putObject(
        PutObjectRequest
          .builder()
          .bucket(bucketName)
          .key(s"$folder/$fileName")
          .build(),
        RequestBody.fromString(content.render())
      )
    ).toEither.left.map(e => Failure(s"Failed to store '$fileName' in S3: ${e.getMessage}")).map(_ => ())
}
