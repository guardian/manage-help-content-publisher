package managehelpcontentpublisher

import managehelpcontentpublisher.Article.fromInput
import managehelpcontentpublisher.Config.config
import managehelpcontentpublisher.Eithers._
import upickle.default._

import scala.util.Try

case class PathAndContent(path: String, content: String)

object PathAndContent {

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
    * @return List of PathAndContents published.
    *         The meaning of Path depends on the implementation of storeArticle and storeTopic.
    */
  def publishContents(
      fetchArticleByPath: String => Either[Failure, Option[String]],
      fetchTopicByPath: String => Either[Failure, Option[String]],
      storeArticle: PathAndContent => Either[Failure, PathAndContent],
      storeTopic: PathAndContent => Either[Failure, PathAndContent]
  )(jsonString: String): Either[Failure, Seq[PathAndContent]] = {

    def readInput(jsonString: String): Either[Failure, InputModel] =
      Try(read[InputModel](jsonString)).toEither.left.map(e => Failure(s"Failed to read input: ${e.getMessage}"))

    def readArticle(jsonString: String): Either[Failure, Article] =
      Try(read[Article](jsonString)).toEither.left.map(e => Failure(s"Failed to read article: ${e.getMessage}"))

    def writeTopic(topic: Topic): Either[Failure, String] =
      Try(write(topic)).toEither.left.map(e => Failure(s"Failed to write topic: ${e.getMessage}"))

    def publishArticle(article: Article): Either[Failure, PathAndContent] = {
      def writeArticle(article: Article): Either[Failure, String] =
        Try(write(article)).toEither.left.map(e => Failure(s"Failed to write article: ${e.getMessage}"))
      for {
        content <- writeArticle(article)
        result <- storeArticle(PathAndContent(article.path, content))
      } yield result
    }

    def publishTopic(topic: Topic): Either[Failure, PathAndContent] = {
      for {
        content <- writeTopic(topic)
        result <- storeTopic(PathAndContent(topic.path, content))
      } yield result
    }

    def removeFromTopics(article: Article, topics: Seq[ArticleTopic]): Either[Failure, Seq[PathAndContent]] =
      seqToEither(topics.map(removeFromTopic(article))).map(_.flatten)

    def removeFromTopic(article: Article)(articleTopic: ArticleTopic): Either[Failure, Option[PathAndContent]] = {

      def readTopic(jsonString: String): Either[Failure, Topic] =
        Try(read[Topic](jsonString)).toEither.left.map(e => Failure(s"Failed to read topic: ${e.getMessage}"))

      for {
        oldTopicJson <- fetchTopicByPath(articleTopic.path)
        oldTopic <- optionToEither(oldTopicJson.map(readTopic))
        newTopicJson <- optionToEither(oldTopic.map(Topic.removeFromTopic(article)).map(writeTopic))
        newTopic <- optionToEither(newTopicJson.map(content => storeTopic(PathAndContent(articleTopic.path, content))))
      } yield newTopic
    }

    def publishTopics(topics: Seq[Topic]): Either[Failure, Seq[PathAndContent]] =
      seqToEither(topics.map(publishTopic))

    def publishMoreTopics(articleTopics: Seq[Topic]): Either[Failure, Option[PathAndContent]] = {

      def readMoreTopics(jsonString: String): Either[Failure, MoreTopics] =
        Try(read[MoreTopics](jsonString)).toEither.left.map(e =>
          Failure(s"Failed to read more topics from '$jsonString': ${e.getMessage}")
        )

      def writeMoreTopics(moreTopics: MoreTopics): Either[Failure, String] =
        Try(write(moreTopics)).toEither.left.map(e => Failure(s"Failed to write $moreTopics: ${e.getMessage}"))

      def storeMoreTopics(prevMoreTopics: Option[MoreTopics], newContent: Option[String]) = for {
        moreTopics <- prevMoreTopics
        content <- newContent
      } yield storeTopic(PathAndContent(moreTopics.path, content))

      if (articleTopics.forall(topic => config.topic.corePaths.contains(topic.path))) Right(None)
      else
        for {
          jsonString <- fetchTopicByPath(config.topic.moreTopics.path)
          oldMoreTopics <- optionToEither(jsonString.map(readMoreTopics))
          newMoreTopics = MoreTopics.withNewTopics(oldMoreTopics, articleTopics)
          content <- optionToEither(newMoreTopics.map(writeMoreTopics))
          result <- optionToEither(storeMoreTopics(newMoreTopics, content))
        } yield result
    }

    for {
      input <- readInput(jsonString)
      newArticle = fromInput(input.article)
      oldArticleJson <- fetchArticleByPath(newArticle.path)
      oldArticle <- optionToEither(oldArticleJson.map(readArticle))
      publishedArticle <- publishArticle(newArticle)
      topics = Topic.fromInput(input)
      publishedTopics <- publishTopics(topics)
      publishedMoreTopics <- publishMoreTopics(topics)
      topicsArticleRemovedFrom <- removeFromTopics(
        newArticle,
        ArticleTopic.topicsArticleRemovedFrom(newArticle, oldArticle)
      )
    } yield Seq(publishedArticle) ++ publishedTopics ++ publishedMoreTopics.toSeq ++ topicsArticleRemovedFrom
  }
}
