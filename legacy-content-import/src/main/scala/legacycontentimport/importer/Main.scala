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
  * <li>File holding line-separated Capi article paths</li>
  * </ol>
  */
object Main extends App {

  val capiKey = args(0)
  val capiDomain = args(1)
  val articlePathsFile = new File(args(2))

  val article = Article.fromCapiPath(capiDomain, capiKey) _

  val articlePaths = {
    val source = Source.fromFile(articlePathsFile)
    val paths = source.getLines.toList
    source.close()
    paths
  }

  val csvHeader = "UrlName,Title,Body__c"

  val importStructure = articlePaths.foldLeft(ImportStructure(csvHeader, Nil))(ImportStructure.addPath(article))

  private val outputZip = File.createTempFile("legacy-help-articles", ".zip")
  private val properties = new File("legacy-content-import/src/main/resources/import.properties")
  ZipUtil.packEntry(properties, outputZip)
  ZipUtil.addEntry(outputZip, new ByteSource("legacy-help-articles.csv", importStructure.csv.getBytes(UTF_8)))
  importStructure.articleBodies.foreach(ZipUtil.addEntry(outputZip, _))

  println(s"Import file is at $outputZip")
}
