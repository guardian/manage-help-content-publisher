package htmlToJsonTranslator
import org.jsoup.Jsoup
import org.jsoup.nodes.{Element, Node}
import org.jsoup.select.Elements

import scala.jdk.CollectionConverters._
object Main extends App {

  sealed trait HelpArticleTag {
    def `type`: String
  }

  case class TextTag(content: String) extends HelpArticleTag {
    override def `type`: String = "text"
  }
  case class BoldTag() extends HelpArticleTag {
    override def `type`: String = "bold"
  }

  case class ItalicTag() extends HelpArticleTag {
    override def `type`: String = "italic"
  }

  case class ParagraphTag() extends HelpArticleTag {
    override def `type`: String = "paragraph"
  }

  val html =
    "<body><p>my paragraph<b>bold text<i>italic text</i>trailing bold</b></p><p>second paragraph</p></body>"

  processHtml(html, "body")

  def processHtml(html: String, tagName: String): Unit = {
    println("================================")

    val elements: Seq[Element] = getChildElements(html, tagName).asScala.toSeq

    for (element <- elements) {
      val nodes: Seq[Node] = element.childNodes().asScala.toSeq

      for (node <- nodes) {
        processChildNode(node, tagName)
      }
    }
  }

  def processChildNode(node: Node, parentNodeName: String): Any = {

    val generatedTag: HelpArticleTag = generateTag(node)
    println("parent:" + parentNodeName + "  |generatedTag:" + generatedTag)

    if (nodeHasChildren(node)) {

      for (childNode <- node.childNodes().asScala.toSeq) {
        processChildNode(childNode, node.nodeName())
      }
    }
  }

  def generateTag(node: Node): HelpArticleTag = {
    node.nodeName() match {
      case "b"     => BoldTag()
      case "i"     => ItalicTag()
      case "#text" => TextTag(content = node.toString)
      case "p"     => ParagraphTag()
    }
  }

  def nodeHasChildren(node: Node): Boolean = {
    node.childNodes().size() > 0
  }

  def getChildElements(searchIn: String, tagName: String): Elements = {
    Jsoup.parse(searchIn).select(tagName)
  }
}
