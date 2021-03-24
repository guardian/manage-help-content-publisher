package managehelpcontentpublisher

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.{APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent}

object Logging {

  private def log(context: Context, level: String, otherFields: ujson.Obj): Unit =
    context.getLogger.log(ujson.Obj("logLevel" -> level, otherFields.value.toSeq: _*).render())

  private def logInfo(context: Context, event: String, fields: ujson.Obj): Unit =
    log(context, "INFO", ujson.Obj("event" -> event, fields.value.toSeq: _*))

  def logError(context: Context, message: String): Unit = log(context, "ERROR", ujson.Obj("message" -> message))

  def logRequest(context: Context, request: APIGatewayProxyRequestEvent): Unit =
    logInfo(context, "Request", ujson.Obj("body" -> request.getBody))

  def logResponse(context: Context, response: APIGatewayProxyResponseEvent): Unit =
    logInfo(context, "Response", ujson.Obj("code" -> response.getStatusCode.toString, "body" -> response.getBody))

  def logPublished(context: Context)(item: PathAndContent): Unit =
    logInfo(context, "Published", ujson.Obj("path" -> item.path, "content" -> item.content))
}
