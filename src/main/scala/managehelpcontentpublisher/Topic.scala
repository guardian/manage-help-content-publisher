package managehelpcontentpublisher

import managehelpcontentpublisher.SalesforceCleaner.cleanCustomFieldName
import upickle.default._

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
}

case class TopicArticle(path: String, title: String)

object TopicArticle {

  implicit val rw: ReadWriter[TopicArticle] = macroRW

  def fromInput(input: InputArticle): TopicArticle = TopicArticle(
    title = input.title,
    path = input.urlName
  )
}
