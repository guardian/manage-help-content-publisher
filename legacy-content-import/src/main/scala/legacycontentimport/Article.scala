package legacycontentimport

import scalaj.http.Http

import java.net.URI
import java.time.Instant
import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

case class Article(url: URI, title: String, publicationDate: Instant)

object Article {

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

    def toArticle(result: ujson.Value): Article = Article(
      title = result("webTitle").str,
      url = new URI(result("webUrl").str),
      publicationDate = Instant.parse(result("webPublicationDate").str)
    )

    val response =
      Http(s"https://$capiDomain/search")
        .param("api-key", capiKey)
        .param("tag", "type/article")
        .param("section", "help")
        .param("page", (pageIndex + 1).toString)
        .asString

    val results = ujson.read(response.body)("response")("results")
    results.arr.toList.map(toArticle)
  }
}
