package legacycontentimport

object Main extends App {

  val capiKey = args(0)
  val capiDomain = args(1)

  val articles = Article.fromCapiHelpSection(capiDomain, capiKey)
  articles foreach { article =>
    val fields = Seq(
      article.url,
      article.title.replace("\"", "'"),
      article.publicationDate,
      article.keywords.mkString(", "),
      article.series.mkString,
      article.tone.mkString,
      article.blog.mkString
    )
    println(fields.mkString("\"", "\",\"", "\""))
  }
}
