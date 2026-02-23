package managehelpcontentpublisher

import org.jsoup._
import org.jsoup.nodes._
import org.jsoup.safety.Safelist
import ujson.Value
import upickle.core.LinkedHashMap

import scala.jdk.CollectionConverters._

object HtmlToJson {

  import managehelpcontentpublisher.Html._

  def apply(html: String): ujson.Value = {
    def naiveLineBreakToParagraph(s: String) = {
      val replaced = html.trim.replaceAll("<br[^>]*>", "</p><p>")
      s"<p>$replaced</p>"
    }
    val body = Jsoup.parseBodyFragment(naiveLineBreakToParagraph(html)).body
    htmlToJson(refined(body)).obj("content")
  }

  private def refined(body: Element): Element =
    (
      unsupportedAttributesRemoved _ andThen
        blankTextNodesRemoved andThen
        emptyParagraphsRemoved
    )(body)

  private def htmlToJson(n: Node): ujson.Value =
    n match {
      case t: TextNode => ujson.Obj("element" -> "text", "content" -> t.text)
      case e: Element  => toJson(e)
    }

  private def toJson(e: Element): ujson.Obj = {
    val htmlAttributes: List[(String, Value)] =
      e.attributes.asList.asScala.toList.map(attribute => attribute.getKey -> attribute.getValue)
    val basicAttributes: List[(String, Value)] = List(
      "element" -> transformed(e.tagName),
      "content" -> e.childNodes.asScala.toList.map(htmlToJson)
    )
    ujson.Obj(LinkedHashMap(basicAttributes ++ htmlAttributes))
  }

  private val elementTransformations = Map("strong" -> "b", "em" -> "i")

  private def transformed(elementName: String): String = elementTransformations.getOrElse(elementName, elementName)
}

object Html {

  def blankTextNodesRemoved(e: Element): Element = {

    def go(acc: Set[TextNode])(curr: Node): Set[TextNode] =
      curr match {
        case t: TextNode if t.isBlank => acc + t
        case _: TextNode              => acc
        case e: Element =>
          e.childNodes.asScala.flatMap(go(acc)).toSet
      }

    val cleaned = e.clone()
    val blankText = go(Set.empty)(cleaned)
    blankText.foreach(_.remove())
    cleaned
  }

  def unsupportedAttributesRemoved(e: Element): Element =
    Jsoup.parseBodyFragment(Jsoup.clean(e.clone().outerHtml, Safelist.relaxed())).body

  def emptyParagraphsRemoved(e: Element): Element = {
    val cleaned = e.clone()
    val emptyParas = cleaned.select("p:empty").asScala.toSet
    for (para <- emptyParas) para.remove()
    cleaned
  }
}
