package managehelpcontentpublisher

import managehelpcontentpublisher.SalesforceCleaner.cleanCustomFieldName
import upickle.default._

case class Topic(path: String, title: String, articles: Seq[TopicArticle])

object Topic {
  implicit val writer: Writer[Topic] = macroW

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

  implicit val writer: Writer[TopicArticle] = macroW

  def fromInput(input: InputArticle): TopicArticle = TopicArticle(
    title = input.title,
    path = input.urlName
  )
}
