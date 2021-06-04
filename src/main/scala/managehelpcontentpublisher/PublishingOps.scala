package managehelpcontentpublisher

/** Operations to read and write content to and from storage accessible to the web layer.
  */
trait PublishingOps {

  /** Fetches the current state of the published article with the given path
    * or None if there is no such article.
    */
  def fetchArticleByPath(path: String): Either[Failure, Option[String]]

  /** Fetches the current state of the published topic with the given path
    * or None if there is no such topic.
    */
  def fetchTopicByPath(path: String): Either[Failure, Option[String]]

  /** Stores the published representation of an article so that it's available to the web layer.
    */
  def storeArticle(pathAndContent: PathAndContent): Either[Failure, PathAndContent]

  /** Stores the published representation of a topic so that it's available to the web layer.
    */
  def storeTopic(pathAndContent: PathAndContent): Either[Failure, PathAndContent]

  /** Deletes the published representation of an article.
    */
  def deleteArticleByPath(path: String): Either[Failure, String]
}
