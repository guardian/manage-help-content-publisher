package managehelpcontentpublisher

import upickle.default._

import scala.util.Try

case class InputModel(article: InputArticle, dataCategories: Seq[DataCategory])

object InputModel {
  implicit val reader: Reader[InputModel] = macroR

  def readInput(jsonString: String): Either[Failure, InputModel] =
    Try(read[InputModel](jsonString)).toEither.left.map(e => Failure(s"Failed to read input: ${e.getMessage}"))
}

case class InputArticle(
    title: String,
    body: String,
    urlName: String,
    dataCategories: Seq[ArticleDataCategory]
)

object InputArticle {
  implicit val reader: Reader[InputArticle] = macroR
}

case class DataCategory(name: String, publishedArticles: Seq[InputArticle])

object DataCategory {
  implicit val reader: Reader[DataCategory] = macroR
}

case class ArticleDataCategory(name: String, label: String)

object ArticleDataCategory {
  implicit val reader: Reader[ArticleDataCategory] = macroR
}
