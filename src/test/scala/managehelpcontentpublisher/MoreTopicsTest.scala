package managehelpcontentpublisher

import managehelpcontentpublisher.Config.config
import utest._

object MoreTopicsTest extends TestSuite {
  def tests: Tests = Tests {

    test("withNewTopics") {

      test("No new topics and no pre-existing more topics") {
        MoreTopics.withNewTopics(oldMoreTopics = None, newTopics = Nil, articleToRemove = None) ==> None
      }

      test("New topic and no pre-existing more topics") {
        val topic = Topic(path = "p", title = "t", articles = Seq(TopicArticle(path = "ap", title = "at")))
        MoreTopics.withNewTopics(
          oldMoreTopics = None,
          newTopics = Seq(topic),
          articleToRemove = None
        ) ==>
          Some(
            MoreTopics(path = config.topic.moreTopics.path, title = config.topic.moreTopics.title, topics = Seq(topic))
          )
      }

      test("New topic and pre-existing more topics") {
        val topic1 = Topic(path = "p1", title = "t1", articles = Seq(TopicArticle(path = "a1p", title = "a1t")))
        val topic2 = Topic(path = "p2", title = "t2", articles = Seq(TopicArticle(path = "a2p", title = "a2t")))
        MoreTopics.withNewTopics(
          oldMoreTopics = Some(
            MoreTopics(
              path = config.topic.moreTopics.path,
              title = config.topic.moreTopics.title,
              topics = Seq(topic1)
            )
          ),
          newTopics = Seq(topic2),
          articleToRemove = None
        ) ==>
          Some(
            MoreTopics(
              path = config.topic.moreTopics.path,
              title = config.topic.moreTopics.title,
              topics = Seq(topic1, topic2)
            )
          )
      }

      test("New topic and pre-existing more topics and an article to remove") {
        val topic1 = Topic(path = "p1", title = "t1", articles = Seq(TopicArticle(path = "a1p", title = "a1t")))
        val topic2 = Topic(path = "p2", title = "t2", articles = Seq(TopicArticle(path = "a2p", title = "a2t")))
        MoreTopics.withNewTopics(
          oldMoreTopics = Some(
            MoreTopics(
              path = config.topic.moreTopics.path,
              title = config.topic.moreTopics.title,
              topics = Seq(topic1)
            )
          ),
          newTopics = Seq(topic2),
          articleToRemove = Some(
            Article(
              title = "a1t",
              body = ujson.Null,
              path = "a1p",
              topics = Seq(ArticleTopic(path = "p1", title = "t1"))
            )
          )
        ) ==>
          Some(
            MoreTopics(
              path = config.topic.moreTopics.path,
              title = config.topic.moreTopics.title,
              topics = Seq(topic1.copy(articles = Nil), topic2)
            )
          )
      }
    }
  }
}
