package managehelpcontentpublisher

import managehelpcontentpublisher.Config.config
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.{
  DeleteObjectRequest,
  GetObjectRequest,
  NoSuchKeyException,
  PutObjectRequest
}

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
        case e                     => Left(ResponseFailure(s"Failed to get s3://${config.aws.bucketName}/$key: ${e.getMessage}"))
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
    ).toEither
      .left
      .map(e => ResponseFailure(s"Failed to put $fullPath: ${e.getMessage}"))
      .map(_ => PathAndContent(fullPath, content))
  }

  private def delete(key: String): Either[Failure, String] = {
    val fullPath = s"s3://${config.aws.bucketName}/$key"
    Try(
      client.deleteObject(
        DeleteObjectRequest
          .builder()
          .bucket(config.aws.bucketName)
          .key(key)
          .build()
      )
    ).toEither
      .left.map(e => ResponseFailure(s"Failed to delete $fullPath: ${e.getMessage}"))
      .map(_ => fullPath)
  }

  val publishingOps: PublishingOps = new PublishingOps {

    def fetchArticleByPath(path: String): Either[Failure, Option[String]] =
      get(s"${config.aws.articlesFolder}/$path.json")

    def fetchTopicByPath(path: String): Either[Failure, Option[String]] =
      get(s"${config.aws.topicsFolder}/$path.json")

    def storeArticle(pathAndContent: PathAndContent): Either[Failure, PathAndContent] =
      put(s"${config.aws.articlesFolder}/${pathAndContent.path}.json", pathAndContent.content)

    def storeTopic(pathAndContent: PathAndContent): Either[Failure, PathAndContent] =
      put(s"${config.aws.topicsFolder}/${pathAndContent.path}.json", pathAndContent.content)

    def deleteArticleByPath(path: String): Either[Failure, String] =
      delete(s"${config.aws.articlesFolder}/$path.json")
  }
}
