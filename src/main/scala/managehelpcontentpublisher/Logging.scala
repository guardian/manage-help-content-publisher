package managehelpcontentpublisher

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.{APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent}
import ujson.{Obj, Str}

import scala.jdk.CollectionConverters._

object Logging {

  private def log(context: Context, level: String, otherFields: Obj): Unit =
    context.getLogger.log(add(otherFields, "logLevel" -> level).render())

  private def logInfo(context: Context, event: String, fields: Obj): Unit =
    log(context, "INFO", add(fields, "event" -> event))

  def logError(context: Context, message: String): Unit = log(context, "ERROR", Obj("message" -> message))

  def logRequest(context: Context, request: APIGatewayProxyRequestEvent): Unit = {
    def toValue(s: Option[String]): ujson.Value = s.map(str => Str(str)).getOrElse(ujson.Null)
    logInfo(
      context,
      "Request",
      Obj(
        "pathParameters" -> toValue(Option(request.getPathParameters.asScala).map(_.toString)),
        "body" -> toValue(Option(request.getBody))
      )
    )
  }

  def logResponse(context: Context, response: APIGatewayProxyResponseEvent): Unit = {
    logInfo(
      context,
      "Response",
      optionallyAdd(Obj("code" -> response.getStatusCode.toString), "body" -> Option(response.getBody))
    )
  }

  def logPublished(context: Context)(item: PathAndContent): Unit =
    logInfo(context, "Published", Obj("path" -> item.path, "content" -> item.content))

  private def add(obj: Obj, field: (String, String)) = Obj(field, obj.value.toSeq: _*)

  private def optionallyAdd(obj: Obj, field: (String, Option[String])) =
    field._2.fold(obj)(value => add(obj, field._1 -> value))
}
