package managehelpcontentpublisher

import org.jsoup._
import org.jsoup.nodes._

import scala.jdk.CollectionConverters._

object HtmlToJson {

  def apply(html: String): ujson.Value = {
    val body = Jsoup.parse(html.trim).body
    htmlToJson(body).obj("content")
  }

  private def htmlToJson(n: Node): ujson.Value =
    n match {
      case t: TextNode => ujson.Obj("text" -> t.text)
      case e: Element  => toJson(e)
    }

  private def toJson(e: Element) = {
    val obj = ujson.Obj(
      "element" -> e.tagName,
      "content" -> e.childNodes.asScala.toList.map(htmlToJson)
    )
    e.attributes.asList.asScala.foldLeft(obj)((acc, attribute) =>
      acc.copy(acc.value ++ Map(attribute.getKey -> attribute.getValue))
    )
  }
}
