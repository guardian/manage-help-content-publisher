package managehelpcontentpublisher

import managehelpcontentpublisher.Article.{readArticle, writeArticle}
import managehelpcontentpublisher.Config.config
import managehelpcontentpublisher.Eithers._
import managehelpcontentpublisher.InputModel.readInput
import managehelpcontentpublisher.MoreTopics.{readMoreTopics, writeMoreTopics}
import managehelpcontentpublisher.Topic.{readTopic, writeTopic}

import java.net.URI

case class PathAndContent(path: String, content: String)

object PathAndContent {

  private def removeFromTopics(
      publishingOps: PublishingOps
  )(article: Article, topics: Seq[ArticleTopic]): Either[Failure, Seq[PathAndContent]] =
    topics.map(removeFromTopic(publishingOps)(article)).sequence.map(_.flatten)

  private def removeFromTopic(
      publishingOps: PublishingOps
  )(article: Article)(articleTopic: ArticleTopic): Either[Failure, Option[PathAndContent]] =
    for {
      oldTopicJson <- publishingOps.fetchTopicByPath(articleTopic.path)
      oldTopic <- oldTopicJson.map(readTopic).sequence
      newTopicJson <- oldTopic.map(Topic.removeFromTopic(article)).map(writeTopic).sequence
      newTopic <- newTopicJson
        .map(content => publishingOps.storeTopic(PathAndContent(articleTopic.path, content))).sequence
    } yield newTopic

  private def publishMoreTopics(
      publishingOps: PublishingOps
  )(oldArticle: Option[Article], newTopics: Seq[Topic]): Either[Failure, Option[PathAndContent]] = {

    def storeMoreTopics(prevMoreTopics: Option[MoreTopics], newContent: Option[String]) = for {
      moreTopics <- prevMoreTopics
      content <- newContent
    } yield publishingOps.storeTopic(PathAndContent(moreTopics.path, content))

    // No need to do anything if the new topics and the topics of the old article are all core topics
    def isCore(path: String) = config.topic.corePaths.contains(path)
    if (
      newTopics.forall(topic => isCore(topic.path)) &&
      oldArticle.forall(_.topics.forall(topic => isCore(topic.path)))
    ) Right(None)
    else
      for {
        jsonString <- publishingOps.fetchTopicByPath(config.topic.moreTopics.path)
        oldMoreTopics <- jsonString.map(readMoreTopics).sequence
        newMoreTopics = MoreTopics.withNewTopics(oldMoreTopics, oldArticle, newTopics)
        content <- newMoreTopics.map(writeMoreTopics).sequence
        result <- storeMoreTopics(newMoreTopics, content).sequence
      } yield result
  }

  /** Publishes the contents of the given Json string
    * to representations of an Article and multiple Topics
    * suitable to be rendered by a web layer.
    *
    * @param publishingOps Operations to read and write content to and from storage accessible to the web layer.
    * @param jsonString A Json object holding all the data needed to publish an Article and its Topics.
    * @return List of PathAndContents published.<br />
    *         The meaning of Path depends on the implementation of publishingOps.
    */
  def publishContents(publishingOps: PublishingOps)(jsonString: String): Either[Failure, Seq[PathAndContent]] = {

    def publishArticle(article: Article): Either[Failure, PathAndContent] =
      for {
        content <- writeArticle(article)
        result <- publishingOps.storeArticle(PathAndContent(article.path, content))
      } yield result

    def publishTopic(topic: Topic): Either[Failure, PathAndContent] =
      for {
        content <- writeTopic(topic)
        result <- publishingOps.storeTopic(PathAndContent(topic.path, content))
      } yield result

    def publishTopics(topics: Seq[Topic]): Either[Failure, Seq[PathAndContent]] =
      topics.map(publishTopic).sequence

    def addToSitemap(article: Article): Either[Failure, Option[PathAndContent]] =
      publishingOps.fetchSitemap() flatMap { oldSitemap =>
        val articleUrl = new URI(s"${config.articleUrlPrefix}/${article.path}")
        if (oldSitemap.contains(articleUrl)) {
          Right(None)
        } else {
          val newSitemap = oldSitemap + articleUrl
          publishingOps.storeSitemap(newSitemap) map (_ =>
            Some(PathAndContent(config.aws.sitemapFile, newSitemap.mkString("\n")))
          )
        }
      }

    for {
      input <- readInput(jsonString)
      newArticle = Article.fromInput(input.article)
      oldArticleJson <- publishingOps.fetchArticleByPath(newArticle.path)
      oldArticle <- oldArticleJson.map(readArticle).sequence
      publishedArticle <- publishArticle(newArticle)
      topics = Topic.fromInput(input)
      publishedTopics <- publishTopics(topics)
      publishedMoreTopics <- publishMoreTopics(publishingOps)(oldArticle, topics)
      topicsArticleRemovedFrom <- removeFromTopics(publishingOps)(
        newArticle,
        ArticleTopic.topicsArticleRemovedFrom(newArticle, oldArticle)
      )
      updatedSitemap <- addToSitemap(newArticle)
    } yield Seq(
      publishedArticle
    ) ++ publishedTopics ++ publishedMoreTopics.toSeq ++ topicsArticleRemovedFrom ++ updatedSitemap.toSeq
  }

  /** Takes down the Article with the given path
    * and removes it from any published Topics and MoreTopics it belonged to.
    *
    * @param publishingOps Operations to read and write content to and from storage accessible to the web layer.
    * @param path Path to published representation of the article.
    * @return List of PathAndContents modified.<br />
    *         The meaning of Path depends on the implementation of deleteArticleByPath and storeTopic.
    */
  def takeDownArticle(publishingOps: PublishingOps)(path: String): Either[Failure, Seq[PathAndContent]] =
    for {
      optArticleJson <- publishingOps.fetchArticleByPath(path)
      articleJson <- optArticleJson.toRight(NotFoundFailure)
      article <- readArticle(articleJson)
      topicsArticleRemovedFrom <- removeFromTopics(publishingOps)(article, article.topics)
      moreTopicsWithoutArticle <- publishMoreTopics(publishingOps)(Some(article), Nil)
      deletedPath <- publishingOps.deleteArticleByPath(path)
    } yield topicsArticleRemovedFrom ++ moreTopicsWithoutArticle.toSeq :+ PathAndContent(deletedPath, "")
}
