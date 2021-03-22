package managehelpcontentpublisher

import managehelpcontentpublisher.Article.fromInput
import upickle.default._

import scala.util.Try

case class PathAndContent(path: String, content: String)

object PathAndContent {

  /** Publishes the contents of the given Json string
    * to representations of an Article and multiple Topics
    * suitable to be rendered by a web layer.
    *
    * @param storeArticle Function that will store the new representation of an Article somewhere.
    * @param storeTopic Function that will store the new representation of a Topic somewhere.
    * @param jsonString A Json string holding all the data needed to publish an Article and its Topics.
    * @return List of PathAndContents published.
    *         The meaning of Path depends on the implementation of storeArticle and storeTopic.
    */
  def publishContents(
      storeArticle: PathAndContent => Either[Failure, PathAndContent],
      storeTopic: PathAndContent => Either[Failure, PathAndContent]
  )(jsonString: String): Either[Failure, Seq[PathAndContent]] = {

    def readInput(jsonString: String): Either[Failure, InputModel] =
      Try(read[InputModel](jsonString)).toEither.left.map(e => Failure(s"Failed to read input: ${e.getMessage}"))

    def publishArticle(article: Article): Either[Failure, PathAndContent] = {
      def writeArticle(article: Article): Either[Failure, String] =
        Try(write(article)).toEither.left.map(e => Failure(s"Failed to write article: ${e.getMessage}"))
      for {
        content <- writeArticle(article)
        result <- storeArticle(PathAndContent(article.path, content))
      } yield result
    }

    def publishTopic(topic: Topic): Either[Failure, PathAndContent] = {
      def writeTopic(topic: Topic): Either[Failure, String] =
        Try(write(topic)).toEither.left.map(e => Failure(s"Failed to write article: ${e.getMessage}"))
      for {
        content <- writeTopic(topic)
        result <- storeTopic(PathAndContent(topic.path, content))
      } yield result
    }

    def publishTopics(input: InputModel): Either[Failure, Seq[PathAndContent]] =
      Eithers.seqToEither(Topic.fromInput(input).map(publishTopic))

    for {
      input <- readInput(jsonString)
      articleResult <- publishArticle(fromInput(input.article))
      topicResults <- publishTopics(input)
    } yield Seq(articleResult) ++ topicResults
  }
}
