package legacycontentimport.importer

import org.zeroturnaround.zip.ByteSource

import java.nio.charset.StandardCharsets.UTF_8

/** <p>The Salesforce Knowledge import requires a CSV file with a row per article.<br />
  * Rich-text fields are treated specially: the CSV field holds a path relative to the CSV file where a HTML file
  * can be found containing the content of the field.</p>
  * <p>The CSV file and all the rich-text files have to be packed together into a zip file.</p>
  *
  * @param csv Multi-line string holding the content of the CSV file
  * @param articleBodies Seq of HTML contents of article body fields
  */
case class ImportStructure(csv: String, articleBodies: Seq[ByteSource])

object ImportStructure {

  def addPath(
      loadArticle: String => ArticleAndTopics
  )(importStructure: ImportStructure, path: String): ImportStructure = {

    val article = loadArticle(path)
    val pathToBody = s"body/${article.resourceName}.html"

    /*
     * See examples in
     * https://help.salesforce.com/articleView?id=sf.knowledge_article_importer_02csv.htm
     */
    val csvRow = Seq(
      article.resourceName,
      article.title,
      pathToBody,
      article.topics.mkString("+")
    ).mkString("\"", "\",\"", "\"")

    ImportStructure(
      csv = s"${importStructure.csv}\n$csvRow",
      articleBodies = importStructure.articleBodies :+ new ByteSource(pathToBody, article.body.getBytes(UTF_8))
    )
  }
}
