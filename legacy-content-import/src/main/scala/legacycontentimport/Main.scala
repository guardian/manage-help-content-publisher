package legacycontentimport

object Main extends App {

  val capiKey = args(0)
  val capiDomain = args(1)

  val articles = Article.fromCapiHelpSection(capiDomain, capiKey)
  articles foreach { article =>
    println(s"""${article.url},"${article.title.replace("\"", "'")}",${article.publicationDate}""")
  }
}
