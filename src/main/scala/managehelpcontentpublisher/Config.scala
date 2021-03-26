package managehelpcontentpublisher

import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.regions.Region.EU_WEST_1

import scala.sys.env

case class Config(stage: String, topic: TopicConfig, aws: AwsConfig)

case class TopicConfig(corePaths: Set[String], moreTopics: MoreTopicsConfig)

case class MoreTopicsConfig(path: String, title: String)

case class AwsConfig(region: Region, bucketName: String, articlesFolder: String, topicsFolder: String)

object Config {

  private val stage = env.getOrElse("stage", "DEV")

  val config: Config = Config(
    stage,
    TopicConfig(
      corePaths = Set(
        "billing",
        "delivery",
        "accounts",
        "journalism",
        "subscriptions",
        "website"
      ),
      moreTopics = MoreTopicsConfig(
        path = "more-topics",
        title = "More topics"
      )
    ),
    AwsConfig(
      region = EU_WEST_1,
      bucketName = "manage-help-content",
      articlesFolder = s"$stage/articles",
      topicsFolder = s"$stage/topics"
    )
  )
}
