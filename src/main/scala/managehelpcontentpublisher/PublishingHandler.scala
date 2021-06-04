package managehelpcontentpublisher

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.{APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent}
import managehelpcontentpublisher.Handler.buildResponse
import managehelpcontentpublisher.Logging._

import java.io.File
import scala.util.chaining.scalaUtilChainingOps

object PublishingHandler {

  def handleRequest(request: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent =
    logRequest(context, request)
      .pipe(_ => buildResponse(context, publishContents(request.getBody)))
      .tap(logResponse(context, _))

  def main(args: Array[String]): Unit =
    Handler.main(publishContents, new File(args(0)))

  private def publishContents(jsonString: String): Either[Failure, Seq[PathAndContent]] =
    PathAndContent.publishContents(S3.publishingOps)(jsonString)
}
