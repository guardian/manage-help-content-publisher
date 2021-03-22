package managehelpcontentpublisher

import org.jsoup._
import org.jsoup.nodes._
import org.jsoup.safety.Whitelist

import scala.jdk.CollectionConverters._

object HtmlToJson {

  def apply(html: String): ujson.Value = {
    val body = Jsoup.parseBodyFragment(html.trim).body
    htmlToJson(refined(body)).obj("content")
  }

  private def refined(body: Element): Element = {

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
      Jsoup.parseBodyFragment(Jsoup.clean(e.outerHtml, Whitelist.relaxed())).body

    def lineBreaksRemoved(e: Element): Element = {

      def remove(br: Element): Unit = {
        Option(br.previousSibling).foreach(_.wrap("<p>"))
        Option(br.nextSibling).foreach(_.wrap("<p>"))
        br.remove()
      }

      val cleaned = e.clone()
      val lineBreaks = cleaned.select("br").asScala.toSet
      for (break <- lineBreaks) remove(break)
      cleaned
    }

    def emptyParagraphsRemoved(e: Element): Element = {
      val cleaned = e.clone()
      val emptyParas = cleaned.select("p:empty").asScala.toSet
      for (para <- emptyParas) para.remove()
      cleaned
    }

    (
      lineBreaksRemoved _ andThen
        unsupportedAttributesRemoved andThen
        blankTextNodesRemoved andThen
        emptyParagraphsRemoved
    )(body)
  }

  private def htmlToJson(n: Node): ujson.Value =
    n match {
      case t: TextNode => ujson.Obj("element" -> "text", "content" -> t.text)
      case e: Element  => toJson(e)
    }

  private def toJson(e: Element): ujson.Obj = {
    val obj = ujson.Obj(
      "element" -> transformed(e.tagName),
      "content" -> e.childNodes.asScala.toList.map(htmlToJson)
    )
    e.attributes.asList.asScala.foldLeft(obj)((acc, attribute) =>
      acc.copy(acc.value ++ Map(attribute.getKey -> attribute.getValue))
    )
  }

  private val elementTransformations = Map("strong" -> "b", "em" -> "i")

  private def transformed(elementName: String): String = elementTransformations.getOrElse(elementName, elementName)
}
