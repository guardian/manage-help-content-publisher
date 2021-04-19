package managehelpcontentpublisher

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import managehelpcontentpublisher.Logging.{logError, logPublished}
import managehelpcontentpublisher.PublishingHandler.publishContents

import java.io.File
import scala.io.Source

object Handler {

  def main(process: String => Either[Failure, Seq[PathAndContent]], in: File): Unit = {
    val src = Source.fromFile(in)
    val input = src.mkString
    src.close()
    process(input) match {
      case Left(e) => println(s"Failed: ${e.reason}")
      case Right(published) =>
        println(s"Success!")
        published.foreach(item => println(s"Wrote to ${item.path}: ${item.content}"))
    }
  }

  def buildResponse(context: Context, result: Either[Failure, Seq[PathAndContent]]): APIGatewayProxyResponseEvent =
    result match {
      case Left(RequestFailure(reason)) =>
        logError(context, reason)
        new APIGatewayProxyResponseEvent().withStatusCode(400).withBody(reason)
      case Left(NotFoundFailure) =>
        logError(context, NotFoundFailure.reason)
        new APIGatewayProxyResponseEvent().withStatusCode(404).withBody(NotFoundFailure.reason)
      case Left(ResponseFailure(reason)) =>
        logError(context, reason)
        new APIGatewayProxyResponseEvent().withStatusCode(500).withBody(reason)
      case Right(published) =>
        published.foreach(logPublished(context))
        new APIGatewayProxyResponseEvent().withStatusCode(204)
    }
}
