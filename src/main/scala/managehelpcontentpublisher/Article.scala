package managehelpcontentpublisher

import upickle.default._

case class Article(title: String, body: String, path: String, topics: Seq[Topic])

object Article {
  implicit val reader: Reader[Article] = macroR
}
