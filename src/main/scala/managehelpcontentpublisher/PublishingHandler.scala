package managehelpcontentpublisher

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.{APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent}
import managehelpcontentpublisher.Handler.buildResponse
import managehelpcontentpublisher.Logging._

import java.io.File

object PublishingHandler {

  def handleRequest(request: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent = {
    logRequest(context, request)
    val response = buildResponse(context, publishContents(request.getBody))
    logResponse(context, response)
    response
  }

  def main(args: Array[String]): Unit =
    Handler.main(publishContents, new File(args(0)))

  private def publishContents(jsonString: String): Either[Failure, Seq[PathAndContent]] =
    PathAndContent.publishContents(S3.fetchArticleByPath, S3.fetchTopicByPath, S3.putArticle, S3.putTopic)(jsonString)
}
