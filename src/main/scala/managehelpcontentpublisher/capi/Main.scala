package managehelpcontentpublisher.capi

import upickle.default.write

object Main extends App {

  val capiKey = args(0)
  val capiDomain = args(1)
  val path = args(2)

  val article = Article.fromCapiPath(capiDomain, capiKey) _
  val tag = Tag.fromCapiPath(capiDomain, capiKey) _

  val generated =
    if (path.contains('/'))
      write[Article](article(path))
    else
      write[Tag](tag(path))

  println(generated)
}
