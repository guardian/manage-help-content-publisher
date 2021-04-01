package managehelpcontentpublisher

import managehelpcontentpublisher.SalesforceCleaner.cleanCustomFieldName
import upickle.default._

case class Article(title: String, body: ujson.Value, path: String, topics: Seq[ArticleTopic])

object Article {

  implicit val rw: ReadWriter[Article] = macroRW

  def fromInput(input: InputArticle): Article = Article(
    title = input.title,
    body = HtmlToJson(input.body),
    path = input.urlName,
    topics = input.dataCategories.map(ArticleTopic.fromSalesforceDataCategory)
  )
}

case class ArticleTopic(path: String, title: String)

object ArticleTopic {

  implicit val rw: ReadWriter[ArticleTopic] = macroRW

  def fromSalesforceDataCategory(cat: ArticleDataCategory): ArticleTopic = ArticleTopic(
    path = cleanCustomFieldName(cat.name),
    title = cat.label
  )

  /** Gives the list of topics that have been removed between the two given versions of an article.
    * If there is no previous version, gives an empty list.
    */
  def topicsArticleRemovedFrom(curr: Article, prev: Option[Article]): Seq[ArticleTopic] =
    prev.map(_.topics.diff(curr.topics)).getOrElse(Nil)
}
