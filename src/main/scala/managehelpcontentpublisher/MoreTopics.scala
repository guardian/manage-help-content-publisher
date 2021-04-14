package managehelpcontentpublisher

import managehelpcontentpublisher.Config.config
import upickle.default._

import scala.util.Try

/** A compilation of published articles by topic in the non-core topics.
  */
case class MoreTopics(path: String, title: String, topics: Seq[Topic])

object MoreTopics {

  implicit val rw: ReadWriter[MoreTopics] = macroRW

  def apply(topics: Seq[Topic]): MoreTopics = MoreTopics(
    path = config.topic.moreTopics.path,
    title = config.topic.moreTopics.title,
    topics
  )

  /** Creates new MoreTopics with a combination of the existing topic lists and the given new topic lists.
    * Core topics are not included in MoreTopics.
    *
    * @param oldMoreTopics Base for the new MoreTopics if there is a pre-existing instance.
    * @param articleToRemove An article to remove from all topic lists before adding new topics.
    * @param newTopics New topic lists to add to the base list, replacing any lists with the same paths.
    * @return New instance combining the old instance and the new lists.
    */
  def withNewTopics(
      oldMoreTopics: Option[MoreTopics],
      articleToRemove: Option[Article],
      newTopics: Seq[Topic]
  ): Option[MoreTopics] = {
    def removeArticle(topic: Topic): Topic = articleToRemove.fold(topic) { article =>
      topic.copy(articles = topic.articles.filterNot(_.path == article.path))
    }
    def toMap(topics: Seq[Topic]) = topics.map(topic => topic.path -> topic).toMap
    val topics = oldMoreTopics
      .fold(newTopics) { moreTopics =>
        val combinedTopics = toMap(moreTopics.topics.map(removeArticle)) ++ toMap(newTopics)
        combinedTopics.values.toSeq
      }
      .filterNot(topic => config.topic.corePaths.contains(topic.path))
      .sortBy(_.title)
    if (topics.isEmpty) None else Some(MoreTopics(topics))
  }

  def readMoreTopics(jsonString: String): Either[Failure, MoreTopics] =
    Try(read[MoreTopics](jsonString)).toEither.left.map(e =>
      Failure(s"Failed to read more topics from '$jsonString': ${e.getMessage}")
    )

  def writeMoreTopics(moreTopics: MoreTopics): Either[Failure, String] =
    Try(write(moreTopics)).toEither.left.map(e => Failure(s"Failed to write $moreTopics: ${e.getMessage}"))
}
