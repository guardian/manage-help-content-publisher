package legacycontentimport

import scalaj.http.Http

import java.net.URI
import java.time.Instant

case class Article(
    url: URI,
    title: String,
    publicationDate: Instant,
    keywords: Seq[String],
    series: Option[String],
    tone: Option[String],
    blog: Option[String]
)

object Article {

  private val excludedTags = Seq("help/help", "tone/help")

  private case class ArticlesAndPageCount(articles: Seq[Article], pageCount: Int)

  def fromCapiHelpSection(capiDomain: String, capiKey: String): Seq[Article] = {
    val fetch = pageFromCapiHelpSection(capiDomain, capiKey) _
    val articlesAndPageCount = firstPageFromCapiHelpSection(capiDomain, capiKey)
    val pageIndices = 1 until articlesAndPageCount.pageCount
    val firstPageArticles = articlesAndPageCount.articles
    pageIndices.foldLeft(firstPageArticles)((acc, pageIndex) => acc ++ fetch(pageIndex))
  }

  private def firstPageFromCapiHelpSection(capiDomain: String, capiKey: String): ArticlesAndPageCount = {
    val capiResponse = capiRequest(capiDomain, capiKey).asString
    val response = ujson.read(capiResponse.body)("response")
    ArticlesAndPageCount(
      articles = response("results").arr.toList.map(toArticle),
      pageCount = response("pages").num.toInt
    )
  }

  private def pageFromCapiHelpSection(capiDomain: String, capiKey: String)(pageIndex: Int): Seq[Article] = {
    val capiResponse =
      capiRequest(capiDomain, capiKey)
        .param("page", (pageIndex + 1).toString)
        .asString
    val results = ujson.read(capiResponse.body)("response")("results")
    results.arr.toList.map(toArticle)
  }

  private def capiRequest(capiDomain: String, capiKey: String) =
    Http(s"https://$capiDomain/search")
      .param("api-key", capiKey)
      .param("tag", "type/article")
      .param("section", "help")
      .param("show-tags", "keyword,series,tone,blog")

  private def toArticle(result: ujson.Value): Article = {

    val tagsToInclude = result("tags").arr.toList.filterNot(tag => excludedTags.contains(tag("id").str))

    def tagsOfType(typeName: String) = tagsToInclude collect {
      case tag if tag("type").str == typeName => tag("webTitle").str
    }

    Article(
      title = result("webTitle").str,
      url = new URI(result("webUrl").str),
      publicationDate = Instant.parse(result("webPublicationDate").str),
      keywords = tagsOfType("keyword").sorted,
      series = tagsOfType("series").headOption,
      tone = tagsOfType("tone").headOption,
      blog = tagsOfType("blog").headOption
    )
  }
}
