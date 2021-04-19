package managehelpcontentpublisher

sealed trait Failure { def reason: String }

case object NotFoundFailure extends Failure {
  val reason: String = "Resource not found"
}

case class RequestFailure(reason: String) extends Failure

case class ResponseFailure(reason: String) extends Failure
