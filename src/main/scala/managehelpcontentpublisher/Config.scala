package managehelpcontentpublisher

import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.regions.Region.EU_WEST_1

import scala.sys.env

case class Config(stage: String, topic: TopicConfig, aws: AwsConfig, articleUrlPrefix: String)

case class TopicConfig(corePaths: Set[String], moreTopics: MoreTopicsConfig)

case class MoreTopicsConfig(path: String, title: String)

case class AwsConfig(
    region: Region,
    bucketName: String,
    articlesFolder: String,
    topicsFolder: String,
    sitemapFile: String
)

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
      topicsFolder = s"$stage/topics",
      sitemapFile = s"$stage/sitemap.txt"
    ),
    articleUrlPrefix = {
      val domain = stage match {
        case "PROD" => "manage.theguardian.com"
        case "CODE" => "manage.code.dev-theguardian.com"
        case _      => "manage.thegulocal.com"
      }
      s"https://$domain/help-centre/article"
    }
  )
}
