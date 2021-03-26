package managehelpcontentpublisher

import managehelpcontentpublisher.Config.config
import upickle.default._

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
    * @param newTopics New topic lists to add to the base list, replacing any lists with the same paths.
    * @return New instance combining the old instance and the new lists.
    */
  def withNewTopics(oldMoreTopics: Option[MoreTopics], newTopics: Seq[Topic]): Option[MoreTopics] = {
    def toMap(topics: Seq[Topic]) = topics.map(topic => topic.path -> topic).toMap
    val topics = oldMoreTopics
      .fold(newTopics) { moreTopics =>
        val combinedTopics = toMap(moreTopics.topics) ++ toMap(newTopics)
        combinedTopics.values.toSeq
      }
      .filterNot(topic => config.topic.corePaths.contains(topic.path))
      .sortBy(_.title)
    if (topics.isEmpty) None else Some(MoreTopics(topics))
  }
}
