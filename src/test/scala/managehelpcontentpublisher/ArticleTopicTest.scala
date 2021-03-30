package managehelpcontentpublisher

import utest._

object ArticleTopicTest extends TestSuite {

  object Fixtures {

    val billingTopic: ArticleTopic = ArticleTopic("billing", "Billing")
    val deliveryTopic: ArticleTopic = ArticleTopic("delivery", "Delivery")

    def mkArticle(topics: ArticleTopic*): Article = Article(
      title = "t",
      body = ujson.Str("body"),
      path = "p",
      topics
    )
  }

  def tests: Tests = Tests {
    import Fixtures._

    test("topicsArticleRemovedFrom") {
      test("when article hasn't been removed from any topics") {
        val prev = mkArticle(billingTopic)
        val curr = mkArticle(billingTopic)
        ArticleTopic.topicsArticleRemovedFrom(curr, Some(prev)) ==> Nil
      }
      test("when article has been removed from a topic") {
        val prev = mkArticle(deliveryTopic)
        val curr = mkArticle(billingTopic)
        ArticleTopic.topicsArticleRemovedFrom(curr, Some(prev)) ==> Seq(deliveryTopic)
      }
      test("when article has been removed from multiple topics") {
        val prev = mkArticle(billingTopic, deliveryTopic)
        val curr = mkArticle()
        ArticleTopic.topicsArticleRemovedFrom(curr, Some(prev)) ==> Seq(billingTopic, deliveryTopic)
      }
      test("when some topics have been added") {
        val prev = mkArticle()
        val curr = mkArticle(billingTopic, deliveryTopic)
        ArticleTopic.topicsArticleRemovedFrom(curr, Some(prev)) ==> Nil
      }
      test("when some topics have been added and removed") {
        val prev = mkArticle(deliveryTopic)
        val curr = mkArticle(billingTopic)
        ArticleTopic.topicsArticleRemovedFrom(curr, Some(prev)) ==> Seq(deliveryTopic)
      }
      test("when there's no previous version") {
        val curr = mkArticle(billingTopic)
        ArticleTopic.topicsArticleRemovedFrom(curr, None) ==> Nil
      }
    }
  }
}
