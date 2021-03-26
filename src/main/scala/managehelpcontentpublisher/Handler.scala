package managehelpcontentpublisher

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.{APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent}

import java.io.File
import scala.io.Source
import Logging._

object Handler {

  def handleRequest(request: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent = {
    logRequest(context, request)
    val response = publishContents(request.getBody) match {
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
    publishContents(input) match {
      case Left(e) => println(s"Failed: ${e.reason}")
      case Right(published) =>
        println(s"Success!")
        published.foreach(item => println(s"Wrote to ${item.path}: ${item.content}"))
    }
  }

  private def publishContents(jsonString: String): Either[Failure, Seq[PathAndContent]] =
    PathAndContent.publishContents(S3.putArticle, S3.putTopic, S3.fetchMoreTopics())(jsonString)
}
