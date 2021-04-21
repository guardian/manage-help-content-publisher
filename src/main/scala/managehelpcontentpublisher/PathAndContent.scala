package managehelpcontentpublisher

import managehelpcontentpublisher.Article.{readArticle, writeArticle}
import managehelpcontentpublisher.Config.config
import managehelpcontentpublisher.Eithers._
import managehelpcontentpublisher.InputModel.readInput
import managehelpcontentpublisher.MoreTopics.{readMoreTopics, writeMoreTopics}
import managehelpcontentpublisher.Topic.{readTopic, writeTopic}

case class PathAndContent(path: String, content: String)

object PathAndContent {

  private def removeFromTopics(
      fetchTopicByPath: String => Either[Failure, Option[String]],
      storeTopic: PathAndContent => Either[Failure, PathAndContent]
  )(article: Article, topics: Seq[ArticleTopic]): Either[Failure, Seq[PathAndContent]] =
    topics.map(removeFromTopic(fetchTopicByPath, storeTopic)(article)).sequence.map(_.flatten)

  private def removeFromTopic(
      fetchTopicByPath: String => Either[Failure, Option[String]],
      storeTopic: PathAndContent => Either[Failure, PathAndContent]
  )(article: Article)(articleTopic: ArticleTopic): Either[Failure, Option[PathAndContent]] =
    for {
      oldTopicJson <- fetchTopicByPath(articleTopic.path)
      oldTopic <- oldTopicJson.map(readTopic).sequence
      newTopicJson <- oldTopic.map(Topic.removeFromTopic(article)).map(writeTopic).sequence
      newTopic <- newTopicJson.map(content => storeTopic(PathAndContent(articleTopic.path, content))).sequence
    } yield newTopic

  private def publishMoreTopics(
      fetchTopicByPath: String => Either[Failure, Option[String]],
      storeTopic: PathAndContent => Either[Failure, PathAndContent]
  )(oldArticle: Option[Article], newTopics: Seq[Topic]): Either[Failure, Option[PathAndContent]] = {

    def storeMoreTopics(prevMoreTopics: Option[MoreTopics], newContent: Option[String]) = for {
      moreTopics <- prevMoreTopics
      content <- newContent
    } yield storeTopic(PathAndContent(moreTopics.path, content))

    // No need to do anything if the new topics and the topics of the old article are all core topics
    def isCore(path: String) = config.topic.corePaths.contains(path)
    if (
      newTopics.forall(topic => isCore(topic.path)) &&
      oldArticle.forall(_.topics.forall(topic => isCore(topic.path)))
    ) Right(None)
    else
      for {
        jsonString <- fetchTopicByPath(config.topic.moreTopics.path)
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
    * @param fetchArticleByPath Function that fetches the current state of the published article with the given path
    *                        or None if there is no such article.
    * @param fetchTopicByPath Function that fetches the current state of the published topic with the given path
    *                        or None if there is no such topic.
    * @param storeArticle Function that will store the new representation of an Article somewhere.
    * @param storeTopic Function that will store the new representation of a Topic somewhere.
    * @param jsonString A Json string holding all the data needed to publish an Article and its Topics.
    * @return List of PathAndContents published.<br />
    *         The meaning of Path depends on the implementation of storeArticle and storeTopic.
    */
  def publishContents(
      fetchArticleByPath: String => Either[Failure, Option[String]],
      fetchTopicByPath: String => Either[Failure, Option[String]],
      storeArticle: PathAndContent => Either[Failure, PathAndContent],
      storeTopic: PathAndContent => Either[Failure, PathAndContent]
  )(jsonString: String): Either[Failure, Seq[PathAndContent]] = {

    def publishArticle(article: Article): Either[Failure, PathAndContent] =
      for {
        content <- writeArticle(article)
        result <- storeArticle(PathAndContent(article.path, content))
      } yield result

    def publishTopic(topic: Topic): Either[Failure, PathAndContent] =
      for {
        content <- writeTopic(topic)
        result <- storeTopic(PathAndContent(topic.path, content))
      } yield result

    def publishTopics(topics: Seq[Topic]): Either[Failure, Seq[PathAndContent]] =
      topics.map(publishTopic).sequence

    for {
      input <- readInput(jsonString)
      newArticle = Article.fromInput(input.article)
      oldArticleJson <- fetchArticleByPath(newArticle.path)
      oldArticle <- oldArticleJson.map(readArticle).sequence
      publishedArticle <- publishArticle(newArticle)
      topics = Topic.fromInput(input)
      publishedTopics <- publishTopics(topics)
      publishedMoreTopics <- publishMoreTopics(fetchTopicByPath, storeTopic)(oldArticle, topics)
      topicsArticleRemovedFrom <- removeFromTopics(fetchTopicByPath, storeTopic)(
        newArticle,
        ArticleTopic.topicsArticleRemovedFrom(newArticle, oldArticle)
      )
    } yield Seq(publishedArticle) ++ publishedTopics ++ publishedMoreTopics.toSeq ++ topicsArticleRemovedFrom
  }

  /** Takes down the Article with the given path
    * and removes it from any published Topics and MoreTopics it belonged to.
    *
    * @param fetchArticleByPath Function that fetches the current state of the published article with the given path
    *                        or None if there is no such article.
    * @param deleteArticleByPath Function that will delete the published representation of an Article.
    * @param fetchTopicByPath Function that fetches the current state of the published topic with the given path
    *                        or None if there is no such topic.
    * @param storeTopic Function that will store the new representation of a Topic somewhere.
    * @param path Path to published representation of the article.
    * @return List of PathAndContents modified.<br />
    *         The meaning of Path depends on the implementation of deleteArticleByPath and storeTopic.
    */
  def takeDownArticle(
      fetchArticleByPath: String => Either[Failure, Option[String]],
      deleteArticleByPath: String => Either[Failure, String],
      fetchTopicByPath: String => Either[Failure, Option[String]],
      storeTopic: PathAndContent => Either[Failure, PathAndContent]
  )(path: String): Either[Failure, Seq[PathAndContent]] =
    for {
      optArticleJson <- fetchArticleByPath(path)
      articleJson <- optArticleJson.toRight(NotFoundFailure)
      article <- readArticle(articleJson)
      topicsArticleRemovedFrom <- removeFromTopics(fetchTopicByPath, storeTopic)(article, article.topics)
      moreTopicsWithoutArticle <- publishMoreTopics(fetchTopicByPath, storeTopic)(Some(article), Nil)
      deletedPath <- deleteArticleByPath(path)
    } yield topicsArticleRemovedFrom ++ moreTopicsWithoutArticle.toSeq :+ PathAndContent(deletedPath, "")
}
