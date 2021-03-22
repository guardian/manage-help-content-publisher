package managehelpcontentpublisher

import managehelpcontentpublisher.SalesforceCleaner.cleanCustomFieldName
import upickle.default._

case class Article(title: String, body: ujson.Value, path: String, topics: Seq[ArticleTopic])

object Article {

  implicit val writer: Writer[Article] = macroW

  def fromInput(input: InputArticle): Article = Article(
    title = input.title,
    body = HtmlToJson(input.body),
    path = input.urlName,
    topics = input.dataCategories.map(ArticleTopic.fromSalesforceDataCategory)
  )
}

case class ArticleTopic(path: String, title: String)

object ArticleTopic {

  implicit val writer: Writer[ArticleTopic] = macroW

  def fromSalesforceDataCategory(cat: ArticleDataCategory): ArticleTopic = ArticleTopic(
    path = cleanCustomFieldName(cat.name),
    title = cat.label
  )
}
