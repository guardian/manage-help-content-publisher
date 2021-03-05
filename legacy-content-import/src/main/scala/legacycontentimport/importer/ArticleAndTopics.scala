package legacycontentimport.importer

case class ArticleAndTopics(article: Article, topics: Set[String]) {
  val resourceName = article.resourceName
  val title = article.title
  val body = article.body
}
