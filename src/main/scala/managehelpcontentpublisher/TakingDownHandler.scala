package managehelpcontentpublisher

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.{APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent}
import managehelpcontentpublisher.Handler.buildResponse
import managehelpcontentpublisher.Logging._

import java.io.File

object TakingDownHandler {

  def handleRequest(request: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent = {
    logRequest(context, request)
    val response = buildResponse(context, takeDownArticle(request.getPathParameters.get("articlePath")))
    logResponse(context, response)
    response
  }

  def main(args: Array[String]): Unit =
    Handler.main(takeDownArticle, new File(args(0)))

  private def takeDownArticle(path: String): Either[Failure, Seq[PathAndContent]] =
    PathAndContent.takeDownArticle(S3.fetchArticleByPath, S3.deleteArticleByPath, S3.fetchTopicByPath, S3.putTopic)(
      path
    )
}
