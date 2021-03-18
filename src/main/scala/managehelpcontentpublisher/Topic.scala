package managehelpcontentpublisher

import upickle.default._

case class Topic(path: String, name: String)

object Topic {
  implicit val reader: Reader[Topic] = macroR
}
