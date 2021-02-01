package legacycontentimport.importer

import scalaj.http.Http

case class Article(
    resourceName: String,
    title: String,
    body: String
)

object Article {

  def fromCapiPath(capiDomain: String, capiKey: String)(path: String): Article = {

    def resourceNameFromPath(path: String) = path.substring(path.lastIndexOf('/') + 1)

    val responseBody =
      Http(s"https://$capiDomain/$path")
        .param("api-key", capiKey)
        .param("show-fields", "body")
        .asString
        .body

    val content = ujson.read(responseBody)("response")("content")

    Article(
      resourceName = resourceNameFromPath(content("id").str),
      title = content("webTitle").str,
      body = content("fields")("body").str
    )
  }
}
