package managehelpcontentpublisher

import managehelpcontentpublisher.Config.config
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.{GetObjectRequest, NoSuchKeyException, PutObjectRequest}

import java.nio.charset.StandardCharsets.UTF_8
import scala.util.Try

object S3 {

  private val client = S3Client.builder().region(config.aws.region).build()

  private def get(key: String): Either[Failure, Option[String]] =
    Try(
      client
        .getObjectAsBytes(
          GetObjectRequest
            .builder()
            .bucket(config.aws.bucketName)
            .key(key)
            .build()
        )
        .asString(UTF_8)
    ).toEither
      .map(Some(_))
      .left
      .flatMap {
        case _: NoSuchKeyException => Right(None)
        case e                     => Left(Failure(s"Failed to get s3://${config.aws.bucketName}/$key: ${e.getMessage}"))
      }

  private def put(key: String, content: String): Either[Failure, PathAndContent] = {
    val fullPath = s"s3://${config.aws.bucketName}/$key"
    Try(
      client.putObject(
        PutObjectRequest
          .builder()
          .bucket(config.aws.bucketName)
          .key(key)
          .contentType("application/json")
          .build(),
        RequestBody.fromString(content)
      )
    ).toEither.left
      .map(e => Failure(s"Failed to put $fullPath: ${e.getMessage}"))
      .map(_ => PathAndContent(fullPath, content))
  }

  def putArticle(article: PathAndContent): Either[Failure, PathAndContent] =
    put(s"${config.aws.articlesFolder}/${article.path}.json", article.content)

  def putTopic(topic: PathAndContent): Either[Failure, PathAndContent] =
    put(s"${config.aws.topicsFolder}/${topic.path}.json", topic.content)

  def fetchMoreTopics(): Either[Failure, Option[String]] =
    get(s"${config.aws.topicsFolder}/${config.topic.moreTopics.path}.json")
}
