package managehelpcontentpublisher

import managehelpcontentpublisher.SalesforceCleaner.cleanCustomFieldName
import upickle.default._

import scala.util.Try

case class Topic(path: String, title: String, articles: Seq[TopicArticle])

object Topic {
  implicit val rw: ReadWriter[Topic] = macroRW

  def fromInput(input: InputModel): Seq[Topic] = {
    val titles = input.article.dataCategories.map(cat => cat.name -> cat.label).toMap
    input.dataCategories.map(cat =>
      Topic(
        path = cleanCustomFieldName(cat.name),
        title = titles(cat.name),
        articles = cat.publishedArticles.map(TopicArticle.fromInput).sortBy(_.title)
      )
    )
  }

  def readTopic(jsonString: String): Either[Failure, Topic] =
    Try(read[Topic](jsonString)).toEither.left.map(e => ResponseFailure(s"Failed to read topic: ${e.getMessage}"))

  def writeTopic(topic: Topic): Either[Failure, String] =
    Try(write(topic)).toEither.left.map(e => ResponseFailure(s"Failed to write topic: ${e.getMessage}"))

  def removeFromTopic(article: Article)(topic: Topic): Topic =
    topic.copy(articles = topic.articles.filterNot(_.path == article.path))
}

case class TopicArticle(path: String, title: String)

object TopicArticle {

  implicit val rw: ReadWriter[TopicArticle] = macroRW

  def fromInput(input: InputArticle): TopicArticle = TopicArticle(
    title = input.title,
    path = input.urlName
  )
}
