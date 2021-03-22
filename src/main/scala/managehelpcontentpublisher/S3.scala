package managehelpcontentpublisher

import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.regions.Region.EU_WEST_1
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest

import scala.sys.env
import scala.util.Try

object S3 {

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

  private val client = S3Client.builder().region(config.awsConfig.region).build()

  private def put(folder: String)(fileName: String, content: String): Either[Failure, PathAndContent] = {
    val key = s"$folder/$fileName"
    val fullPath = s"${config.awsConfig.bucketName}/$key"
    Try(
      client.putObject(
        PutObjectRequest
          .builder()
          .bucket(config.awsConfig.bucketName)
          .key(key)
          .contentType("application/json")
          .build(),
        RequestBody.fromString(content)
      )
    ).toEither.left
      .map(e => Failure(s"Failed to write to '$fullPath': ${e.getMessage}"))
      .map(_ => PathAndContent(fullPath, content))
  }

  val putArticle: PathAndContent => Either[Failure, PathAndContent] = { article =>
    put(config.awsConfig.articlesFolder)(article.path, article.content)
  }

  val putTopic: PathAndContent => Either[Failure, PathAndContent] = { topic =>
    put(config.awsConfig.topicsFolder)(topic.path, topic.content)
  }
}
