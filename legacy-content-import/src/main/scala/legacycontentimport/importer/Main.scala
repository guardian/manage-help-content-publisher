package legacycontentimport.importer

import org.zeroturnaround.zip.{ByteSource, ZipUtil}

import java.io.File
import java.nio.charset.StandardCharsets.UTF_8
import scala.io.Source

/** Generates a Salesforce Knowledge import file.
  *
  * Expected commandline args:
  * <ol>
  * <li>Capi key</li>
  * <li>Capi domain</li>
  * <li>CSV file holding a row for each Capi article path and its new Help Centre topics<br />
  * For example:<br />
  * <code>help/2021/feb/08/getting-started-with-your-digital-subscription,The_Guardian_apps,Billing</code>
  * </li>
  * </ol>
  */
object Main extends App {

  val capiKey = args(0)
  val capiDomain = args(1)
  val articlePathsFile = new File(args(2))

  def article(articleData: String): ArticleAndTopics = {
    val dataFields = articleData.split(",")
    ArticleAndTopics(
      article = Article.fromCapiPath(capiDomain, capiKey)(dataFields.head),
      topics = dataFields.tail.toSet
    )
  }

  val articleData = {
    val source = Source.fromFile(articlePathsFile)
    val rows = source.getLines.toList
    source.close()
    rows
  }

  /*
   * See
   * https://help.salesforce.com/articleView?id=sf.knowledge_article_importer_02csv.htm
   */
  val csvHeader = "UrlName,Title,Body__c,DataCategoryGroup.Help_Centre_Topics"

  val importStructure = articleData.foldLeft(ImportStructure(csvHeader, Nil))(ImportStructure.addPath(article))

  private val outputZip = File.createTempFile("legacy-help-articles", ".zip")
  private val properties = new File("legacy-content-import/src/main/resources/import.properties")
  ZipUtil.packEntry(properties, outputZip)
  ZipUtil.addEntry(outputZip, new ByteSource("legacy-help-articles.csv", importStructure.csv.getBytes(UTF_8)))
  importStructure.articleBodies.foreach(ZipUtil.addEntry(outputZip, _))

  println(s"Import file is at $outputZip")
}
