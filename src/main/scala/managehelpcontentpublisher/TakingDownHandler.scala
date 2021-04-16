package managehelpcontentpublisher

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.{APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent}
import managehelpcontentpublisher.Logging._

import java.io.File
import scala.io.Source

object TakingDownHandler {

  def handleRequest(request: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent = {
    logRequest(context, request)
    val response = takeDownArticle(request.getBody) match {
      case Left(e) =>
        logError(context, e.reason)
        new APIGatewayProxyResponseEvent().withStatusCode(500).withBody(e.reason)
      case Right(published) =>
        published.foreach(logPublished(context))
        new APIGatewayProxyResponseEvent().withStatusCode(204)
    }
    logResponse(context, response)
    response
  }

  def main(args: Array[String]): Unit = {
    val inFile = Source.fromFile(new File(args(0)))
    val input = inFile.mkString
    inFile.close()
    takeDownArticle(input) match {
      case Left(e) => println(s"Failed: ${e.reason}")
      case Right(modified) =>
        println(s"Success!")
        modified.foreach(item => println(s"Wrote to ${item.path}: ${item.content}"))
    }
  }

  private def takeDownArticle(jsonString: String): Either[Failure, Seq[PathAndContent]] =
    PathAndContent.takeDownArticle(S3.deleteArticleByPath, S3.fetchTopicByPath, S3.putTopic)(jsonString)
}
