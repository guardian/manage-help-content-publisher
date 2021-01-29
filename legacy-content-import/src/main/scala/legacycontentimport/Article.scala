package legacycontentimport

import scalaj.http.Http

import java.net.URI
import java.time.Instant
import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

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

  val excludedTags = Seq("help/help", "tone/help")

  def fromCapiHelpSection(capiDomain: String, capiKey: String): Seq[Article] = {

    val fetch = fromCapiHelpSectionPage(capiDomain, capiKey) _

    @tailrec
    def go(pageIndex: Int, acc: Seq[Article]): Seq[Article] =
      Try(fetch(pageIndex)) match {
        case Failure(_)        => acc
        case Success(articles) => go(pageIndex + 1, acc ++ articles)
      }

    go(0, Nil)
  }

  private def fromCapiHelpSectionPage(capiDomain: String, capiKey: String)(pageIndex: Int): Seq[Article] = {

    def toArticle(result: ujson.Value): Article = {

      val tagsToInclude = result("tags").arr.toList.filterNot(tag => excludedTags.contains(tag("id").str))

      def tagsOfType(typeName: String) =
        tagsToInclude.filter(tag => tag("type").str == typeName).map(_("webTitle").str)

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

    val response =
      Http(s"https://$capiDomain/search")
        .param("api-key", capiKey)
        .param("tag", "type/article")
        .param("section", "help")
        .param("show-tags", "keyword,series,tone,blog")
        .param("page", (pageIndex + 1).toString)
        .asString

    val results = ujson.read(response.body)("response")("results")
    results.arr.toList.map(toArticle)
  }
}
