package managehelpcontentpublisher

import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest

import scala.util.Try

object S3 {

  def put(s3: S3Client, bucketName: String, folder: String)(
      fileName: String,
      content: String
  ): Either[Failure, Unit] =
    Try(
      s3.putObject(
        PutObjectRequest
          .builder()
          .bucket(bucketName)
          .key(s"$folder/$fileName")
          .contentType("application/json")
          .build(),
        RequestBody.fromString(content)
      )
    ).toEither.left.map(e => Failure(s"Failed to store '$fileName' in S3: ${e.getMessage}")).map(_ => ())
}
