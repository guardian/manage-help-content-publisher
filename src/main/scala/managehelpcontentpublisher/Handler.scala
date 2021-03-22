package managehelpcontentpublisher

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.{APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent}

import java.io.File
import scala.io.Source

object Handler {

  def handleRequest(event: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent = {

    val logger = context.getLogger
    def logInfo(message: String): Unit = logger.log(s"INFO: $message")
    def logError(message: String): Unit = logger.log(s"ERROR: $message")

    logInfo(s"Input: ${event.getBody}")
    val response = publishContents(event.getBody) match {
      case Left(e) =>
        logError(e.reason)
        new APIGatewayProxyResponseEvent().withStatusCode(500).withBody(e.reason)
      case Right(published) =>
        published.foreach(item => logInfo(s"Wrote to '${item.path}': ${item.content}"))
        new APIGatewayProxyResponseEvent().withStatusCode(204)
    }
    logInfo(s"Response: ${response.toString}")
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
        published.foreach(item => println(s"Wrote to '${item.path}': ${item.content}"))
    }
  }

  private def publishContents(jsonString: String): Either[Failure, Seq[PathAndContent]] =
    PathAndContent.publishContents(S3.putArticle, S3.putTopic)(jsonString)
}
