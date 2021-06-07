package managehelpcontentpublisher

import managehelpcontentpublisher.Config.config
import utest._

import java.net.URI
import scala.io.Source

/** See [[https://github.com/guardian/salesforce/blob/master/force-app/main/default/classes/ArticleBodyValidation.cls#L5-L20 list of HTML elements we support in Salesforce]].
  */
object PathAndContentTestSuite extends TestSuite {

  object Fixtures {

    val article1: (String, String) = "how-can-i-redirect-my-delivery" ->
      """{
        |  "title": "How can I redirect my delivery?",
        |  "body": [
        |    {
        |      "element": "p",
        |      "content": [
        |        {
        |          "element": "text",
        |          "content": "Our customer service team will ..."
        |        }
        |      ]
        |    }
        |  ],
        |  "path": "how-can-i-redirect-my-delivery",
        |  "topics": [
        |    {
        |      "path": "delivery",
        |      "title": "Delivery"
        |    }
        |  ]
        |}""".stripMargin

    val article2: (String, String) = "can-i-read-your-papermagazines-online" ->
      """{
        |  "title": "Can I read your paper/magazines online?",
        |  "body": [
        |    {
        |      "element": "p",
        |      "content": [
        |        {
        |          "element": "text",
        |          "content": "We do not"
        |        }
        |      ]
        |    }
        |  ],
        |  "path": "can-i-read-your-papermagazines-online",
        |  "topics": [
        |    {
        |      "path": "apps",
        |      "title": "The Guardian apps"
        |    }
        |  ]
        |}""".stripMargin

    val deliveryTopic: (String, String) = "delivery" ->
      """{
        |  "path": "delivery",
        |  "title": "Delivery",
        |  "articles": [
        |    {
        |      "path": "can-i-read-your-papermagazines-online",
        |      "title": "Can I read your paper/magazines online?"
        |    },
        |    {
        |      "path": "how-can-i-redirect-my-delivery",
        |      "title": "How can I redirect my delivery?"
        |    },
        |    {
        |      "path": "id-like-to-make-a-complaint-about-an-advertisement",
        |      "title": "I'd like to make a complaint about an advertisement"
        |    },
        |    {
        |      "path": "im-unable-to-comment-and-need-help",
        |      "title": "I'm unable to comment and need help"
        |    }
        |  ]
        |}""".stripMargin

    val appsTopic: (String, String) = "apps" ->
      """{
        |  "path": "apps",
        |  "title": "The Guardian apps",
        |  "articles": [
        |    {
        |      "path": "can-i-read-your-papermagazines-online",
        |      "title": "Can I read your paper/magazines online?"
        |    },
        |    {
        |      "path": "how-can-i-redirect-my-delivery",
        |      "title": "How can I redirect my delivery?"
        |    },
        |    {
        |      "path": "id-like-to-make-a-complaint-about-an-advertisement",
        |      "title": "I'd like to make a complaint about an advertisement"
        |    },
        |    {
        |      "path": "im-unable-to-comment-and-need-help",
        |      "title": "I'm unable to comment and need help"
        |    }
        |  ]
        |}""".stripMargin

    val moreTopics: (String, String) = config.topic.moreTopics.path ->
      """{
      |  "path": "more-topics",
      |  "title": "More Topics",
      |  "topics": [
      |    {
      |      "path": "the-guardian-apps",
      |      "title": "The Guardian apps",
      |      "articles": [
      |        {
      |          "path": "a2",
      |          "title": "Apple/Google subscriptions"
      |        },
      |        {
      |           "path": "can-i-read-your-papermagazines-online",
      |           "title": "Can I read your paper/magazines online?"
      |        },
      |        {
      |          "path": "a3",
      |          "title": "Personalising your apps"
      |        },
      |        {
      |          "path": "a1",
      |          "title": "Premium tier access"
      |        }
      |      ]
      |    },
      |    {
      |      "path": "newsletters-and-emails",
      |      "title": "Newsletters and emails",
      |      "articles": [
      |        {
      |          "path": "n1",
      |          "title": "I'm not receiving any emails from you but think I should be"
      |        },
      |        {
      |          "path": "n2",
      |          "title": "Manage your email preferences"
      |        }
      |      ]
      |    },
      |    {
      |      "path": "events",
      |      "title": "Events",
      |      "articles": [
      |        {
      |          "path": "e1",
      |          "title":
      |            "I can no longer attend the live online event, can I have a refund?"
      |        },
      |        {
      |          "path": "e2",
      |          "title":
      |            "I can’t find my original confirmation email, can you resend me the event link?"
      |        },
      |        {
      |          "path": "e3",
      |          "title":
      |            "Once I have purchased a ticket, how will I attend the online event?"
      |        },
      |        {
      |          "path": "e4",
      |          "title": "I purchased a book with my ticket, when will I receive this?"
      |        }
      |      ]
      |    },
      |    {
      |      "path": "gifting",
      |      "title": "Gifting",
      |      "articles": [
      |        {
      |          "path": "g1",
      |          "title": "Gifting a Digital Subscription"
      |        }
      |      ]
      |    },
      |    {
      |      "path": "archives",
      |      "title": "Back issues and archives",
      |      "articles": [
      |        {
      |          "path": "b1",
      |          "title": "Finding articles from the past in digital format"
      |        },
      |        {
      |          "path": "b2",
      |          "title": "Old newspapers in physical format"
      |        }
      |      ]
      |    }
      |  ]
      |}""".stripMargin

    val sitemap: Set[URI] = Source.fromResource("sitemap.txt").getLines().map(new URI(_)).toSet
  }

  private def publishContents(
      previousArticles: Map[String, String] = Map(),
      previousTopics: Map[String, String] = Map()
  ) =
    PathAndContent.publishContents(
      new PublishingOps {

        def fetchArticleByPath(path: String): Either[Failure, Option[String]] = Right(previousArticles.get(path))

        def fetchTopicByPath(path: String): Either[Failure, Option[String]] = Right(previousTopics.get(path))

        def fetchSitemap(): Either[Failure, Set[URI]] = Right(Fixtures.sitemap)

        def storeArticle(pathAndContent: PathAndContent): Either[Failure, PathAndContent] =
          Right(PathAndContent(s"testArticles/${pathAndContent.path}", pathAndContent.content))

        def storeTopic(pathAndContent: PathAndContent): Either[Failure, PathAndContent] =
          Right(PathAndContent(s"testTopics/${pathAndContent.path}", pathAndContent.content))

        def storeSitemap(urls: Set[URI]): Either[Failure, Unit] = Right(())

        def deleteArticleByPath(path: String): Either[Failure, String] = Left(NotFoundFailure)
      }
    ) _

  private def takeDownArticle(
      articles: Map[String, String] = Map(Fixtures.article1, Fixtures.article2),
      topics: Map[String, String] = Map(Fixtures.deliveryTopic, Fixtures.appsTopic, Fixtures.moreTopics)
  ) =
    PathAndContent.takeDownArticle(
      new PublishingOps {

        def fetchArticleByPath(path: String): Either[Failure, Option[String]] = Right(articles.get(path))

        def fetchTopicByPath(path: String): Either[Failure, Option[String]] = Right(topics.get(path))

        def fetchSitemap(): Either[Failure, Set[URI]] = Right(Fixtures.sitemap)

        def storeArticle(pathAndContent: PathAndContent): Either[Failure, PathAndContent] =
          Left(RequestFailure("unexpected"))

        def storeTopic(pathAndContent: PathAndContent): Either[Failure, PathAndContent] =
          Right(PathAndContent(s"testTopics/${pathAndContent.path}", pathAndContent.content))

        def storeSitemap(urls: Set[URI]): Either[Failure, Unit] = Right(())

        def deleteArticleByPath(path: String): Either[Failure, String] = Right(s"testArticles/$path")
      }
    ) _

  val tests: Tests = Tests {

    test("publishContents") {
      test("When article has a core topic") {
        val published = publishContents()("""{
            |    "dataCategories": [
            |        {
            |            "publishedArticles": [
            |                {
            |                    "urlName": "id-like-to-make-a-complaint-about-an-advertisement",
            |                    "title": "I'd like to make a complaint about an advertisement",
            |                    "id": "id1",
            |                    "dataCategories": [],
            |                    "body": null
            |                },
            |                {
            |                    "urlName": "can-i-read-your-papermagazines-online",
            |                    "title": "Can I read your paper/magazines online?",
            |                    "id": "id2",
            |                    "dataCategories": [],
            |                    "body": null
            |                },
            |                {
            |                    "urlName": "im-unable-to-comment-and-need-help",
            |                    "title": "I'm unable to comment and need help",
            |                    "id": "id3",
            |                    "dataCategories": [],
            |                    "body": null
            |                }
            |            ],
            |            "name": "website__c"
            |        }
            |    ],
            |    "article": {
            |        "urlName": "can-i-read-your-papermagazines-online",
            |        "title": "Can I read your paper/magazines online?",
            |        "id": "id2",
            |        "dataCategories": [
            |            {
            |                "name": "website__c",
            |                "label": "The Guardian website"
            |            }
            |        ],
            |        "body": "<p>We do not</p>"
            |    }
            |}""".stripMargin)
        test("number of files published") {
          published.map(_.length) ==> Right(3)
        }
        test("article published") {
          published.map(_(0)) ==> Right(
            PathAndContent(
              "testArticles/can-i-read-your-papermagazines-online",
              """{"title":"Can I read your paper/magazines online?","body":[{"element":"p","content":[{"element":"text","content":"We do not"}]}],"path":"can-i-read-your-papermagazines-online","topics":[{"path":"website","title":"The Guardian website"}]}"""
            )
          )
        }
        test("topic published") {
          published.map(_(1)) ==> Right(
            PathAndContent(
              "testTopics/website",
              """{"path":"website","title":"The Guardian website","articles":[{"path":"can-i-read-your-papermagazines-online","title":"Can I read your paper/magazines online?"},{"path":"id-like-to-make-a-complaint-about-an-advertisement","title":"I'd like to make a complaint about an advertisement"},{"path":"im-unable-to-comment-and-need-help","title":"I'm unable to comment and need help"}]}"""
            )
          )
        }
        test("sitemap updated") {
          published.map(_(2)) ==> Right(
            PathAndContent(
              "DEV/sitemap.txt",
              """https://manage.thegulocal.com/help-centre/article/article1
                |https://manage.thegulocal.com/help-centre/article/article2
                |https://manage.thegulocal.com/help-centre/article/article3
                |https://manage.thegulocal.com/help-centre/article/can-i-read-your-papermagazines-online""".stripMargin
            )
          )
        }
      }

      test("When article has a non-core topic and 'More topics' is empty") {
        val published = publishContents()("""{
            |    "dataCategories": [
            |        {
            |            "publishedArticles": [
            |                {
            |                    "urlName": "id-like-to-make-a-complaint-about-an-advertisement",
            |                    "title": "I'd like to make a complaint about an advertisement",
            |                    "id": "id1",
            |                    "dataCategories": [],
            |                    "body": null
            |                },
            |                {
            |                    "urlName": "can-i-read-your-papermagazines-online",
            |                    "title": "Can I read your paper/magazines online?",
            |                    "id": "id2",
            |                    "dataCategories": [],
            |                    "body": null
            |                },
            |                {
            |                    "urlName": "im-unable-to-comment-and-need-help",
            |                    "title": "I'm unable to comment and need help",
            |                    "id": "id3",
            |                    "dataCategories": [],
            |                    "body": null
            |                }
            |            ],
            |            "name": "archives__c"
            |        }
            |    ],
            |    "article": {
            |        "urlName": "can-i-read-your-papermagazines-online",
            |        "title": "Can I read your paper/magazines online?",
            |        "id": "id2",
            |        "dataCategories": [
            |            {
            |                "name": "archives__c",
            |                "label": "Back issues and archives"
            |            }
            |        ],
            |        "body": "<p>We do not</p>"
            |    }
            |}""".stripMargin)
        test("number of files published") {
          published.map(_.length) ==> Right(4)
        }
        test("article published") {
          published.map(_(0)) ==> Right(
            PathAndContent(
              "testArticles/can-i-read-your-papermagazines-online",
              """{"title":"Can I read your paper/magazines online?","body":[{"element":"p","content":[{"element":"text","content":"We do not"}]}],"path":"can-i-read-your-papermagazines-online","topics":[{"path":"archives","title":"Back issues and archives"}]}"""
            )
          )
        }
        test("topic published") {
          published.map(_(1)) ==> Right(
            PathAndContent(
              "testTopics/archives",
              """{"path":"archives","title":"Back issues and archives","articles":[{"path":"can-i-read-your-papermagazines-online","title":"Can I read your paper/magazines online?"},{"path":"id-like-to-make-a-complaint-about-an-advertisement","title":"I'd like to make a complaint about an advertisement"},{"path":"im-unable-to-comment-and-need-help","title":"I'm unable to comment and need help"}]}"""
            )
          )
        }
        test("More topics published") {
          published.map(_(2)) ==> Right(
            PathAndContent(
              "testTopics/more-topics",
              """{"path":"more-topics","title":"More topics","topics":[{"path":"archives","title":"Back issues and archives","articles":[{"path":"can-i-read-your-papermagazines-online","title":"Can I read your paper/magazines online?"},{"path":"id-like-to-make-a-complaint-about-an-advertisement","title":"I'd like to make a complaint about an advertisement"},{"path":"im-unable-to-comment-and-need-help","title":"I'm unable to comment and need help"}]}]}"""
            )
          )
        }
        test("sitemap updated") {
          published.map(_(3)) ==> Right(
            PathAndContent(
              "DEV/sitemap.txt",
              """https://manage.thegulocal.com/help-centre/article/article1
                |https://manage.thegulocal.com/help-centre/article/article2
                |https://manage.thegulocal.com/help-centre/article/article3
                |https://manage.thegulocal.com/help-centre/article/can-i-read-your-papermagazines-online""".stripMargin
            )
          )
        }
      }

      test("Article, topic and more topics when article has a non-core topic and 'More topics' is not empty") {
        val published = publishContents(
          previousArticles = Map(Fixtures.article2),
          previousTopics = Map(Fixtures.moreTopics)
        )(
          """{
            |    "dataCategories": [
            |        {
            |            "publishedArticles": [
            |                {
            |                    "urlName": "id-like-to-make-a-complaint-about-an-advertisement",
            |                    "title": "I'd like to make a complaint about an advertisement",
            |                    "id": "id1",
            |                    "dataCategories": [],
            |                    "body": null
            |                },
            |                {
            |                    "urlName": "can-i-read-your-papermagazines-online",
            |                    "title": "Can I read your paper/magazines online?",
            |                    "id": "id2",
            |                    "dataCategories": [],
            |                    "body": null
            |                },
            |                {
            |                    "urlName": "im-unable-to-comment-and-need-help",
            |                    "title": "I'm unable to comment and need help",
            |                    "id": "id3",
            |                    "dataCategories": [],
            |                    "body": null
            |                }
            |            ],
            |            "name": "archives__c"
            |        }
            |    ],
            |    "article": {
            |        "urlName": "can-i-read-your-papermagazines-online",
            |        "title": "Can I read your paper/magazines online?",
            |        "id": "id2",
            |        "dataCategories": [
            |            {
            |                "name": "archives__c",
            |                "label": "Back issues and archives"
            |            }
            |        ],
            |        "body": "<p>We do not</p>"
            |    }
            |}""".stripMargin
        )
        test("number of files published") {
          published.map(_.length) ==> Right(4)
        }
        test("article published") {
          published.map(_(0)) ==> Right(
            PathAndContent(
              "testArticles/can-i-read-your-papermagazines-online",
              """{"title":"Can I read your paper/magazines online?","body":[{"element":"p","content":[{"element":"text","content":"We do not"}]}],"path":"can-i-read-your-papermagazines-online","topics":[{"path":"archives","title":"Back issues and archives"}]}"""
            )
          )
        }
        test("topic published") {
          published.map(_(1)) ==> Right(
            PathAndContent(
              "testTopics/archives",
              """{"path":"archives","title":"Back issues and archives","articles":[{"path":"can-i-read-your-papermagazines-online","title":"Can I read your paper/magazines online?"},{"path":"id-like-to-make-a-complaint-about-an-advertisement","title":"I'd like to make a complaint about an advertisement"},{"path":"im-unable-to-comment-and-need-help","title":"I'm unable to comment and need help"}]}"""
            )
          )
        }
        test("More topics published") {
          published.map(_(2)) ==> Right(
            PathAndContent(
              "testTopics/more-topics",
              """{"path":"more-topics","title":"More topics","topics":[{"path":"archives","title":"Back issues and archives","articles":[{"path":"can-i-read-your-papermagazines-online","title":"Can I read your paper/magazines online?"},{"path":"id-like-to-make-a-complaint-about-an-advertisement","title":"I'd like to make a complaint about an advertisement"},{"path":"im-unable-to-comment-and-need-help","title":"I'm unable to comment and need help"}]},{"path":"events","title":"Events","articles":[{"path":"e1","title":"I can no longer attend the live online event, can I have a refund?"},{"path":"e2","title":"I can’t find my original confirmation email, can you resend me the event link?"},{"path":"e3","title":"Once I have purchased a ticket, how will I attend the online event?"},{"path":"e4","title":"I purchased a book with my ticket, when will I receive this?"}]},{"path":"gifting","title":"Gifting","articles":[{"path":"g1","title":"Gifting a Digital Subscription"}]},{"path":"newsletters-and-emails","title":"Newsletters and emails","articles":[{"path":"n1","title":"I'm not receiving any emails from you but think I should be"},{"path":"n2","title":"Manage your email preferences"}]},{"path":"the-guardian-apps","title":"The Guardian apps","articles":[{"path":"a2","title":"Apple/Google subscriptions"},{"path":"a3","title":"Personalising your apps"},{"path":"a1","title":"Premium tier access"}]}]}"""
            )
          )
        }
        test("sitemap updated") {
          published.map(_(3)) ==> Right(
            PathAndContent(
              "DEV/sitemap.txt",
              """https://manage.thegulocal.com/help-centre/article/article1
                |https://manage.thegulocal.com/help-centre/article/article2
                |https://manage.thegulocal.com/help-centre/article/article3
                |https://manage.thegulocal.com/help-centre/article/can-i-read-your-papermagazines-online""".stripMargin
            )
          )
        }
      }

      test("When topic has changed, publish article and new topic and remove article from old topic") {
        val published = publishContents(
          previousArticles = Map(Fixtures.article1),
          previousTopics = Map(Fixtures.deliveryTopic)
        )(
          """{
            |    "dataCategories": [
            |        {
            |            "publishedArticles": [
            |                {
            |                    "urlName": "id-like-to-make-a-complaint-about-an-advertisement",
            |                    "title": "I'd like to make a complaint about an advertisement",
            |                    "id": "id1",
            |                    "dataCategories": [],
            |                    "body": null
            |                },
            |                {
            |                    "urlName": "how-can-i-redirect-my-delivery",
            |                    "title": "How can I redirect my delivery?",
            |                    "id": "id2",
            |                    "dataCategories": [],
            |                    "body": null
            |                },
            |                {
            |                    "urlName": "im-unable-to-comment-and-need-help",
            |                    "title": "I'm unable to comment and need help",
            |                    "id": "id3",
            |                    "dataCategories": [],
            |                    "body": null
            |                }
            |            ],
            |            "name": "website__c"
            |        }
            |    ],
            |    "article": {
            |        "urlName": "how-can-i-redirect-my-delivery",
            |        "title": "How can I redirect my delivery?",
            |        "id": "id2",
            |        "dataCategories": [
            |            {
            |                "name": "website__c",
            |                "label": "The Guardian website"
            |            }
            |        ],
            |        "body": "<p>We do not</p>"
            |    }
            |}""".stripMargin
        )
        test("number of files published") {
          published.map(_.length) ==> Right(4)
        }
        test("article published") {
          published.map(_(0)) ==> Right(
            PathAndContent(
              "testArticles/how-can-i-redirect-my-delivery",
              """{"title":"How can I redirect my delivery?","body":[{"element":"p","content":[{"element":"text","content":"We do not"}]}],"path":"how-can-i-redirect-my-delivery","topics":[{"path":"website","title":"The Guardian website"}]}"""
            )
          )
        }
        test("topic published") {
          published.map(_(1)) ==> Right(
            PathAndContent(
              "testTopics/website",
              """{"path":"website","title":"The Guardian website","articles":[{"path":"how-can-i-redirect-my-delivery","title":"How can I redirect my delivery?"},{"path":"id-like-to-make-a-complaint-about-an-advertisement","title":"I'd like to make a complaint about an advertisement"},{"path":"im-unable-to-comment-and-need-help","title":"I'm unable to comment and need help"}]}"""
            )
          )
        }
        test("topic republished without article") {
          published.map(_(2)) ==> Right(
            PathAndContent(
              "testTopics/delivery",
              """{"path":"delivery","title":"Delivery","articles":[{"path":"can-i-read-your-papermagazines-online","title":"Can I read your paper/magazines online?"},{"path":"id-like-to-make-a-complaint-about-an-advertisement","title":"I'd like to make a complaint about an advertisement"},{"path":"im-unable-to-comment-and-need-help","title":"I'm unable to comment and need help"}]}"""
            )
          )
        }
        test("sitemap updated") {
          published.map(_(3)) ==> Right(
            PathAndContent(
              "DEV/sitemap.txt",
              """https://manage.thegulocal.com/help-centre/article/article1
                |https://manage.thegulocal.com/help-centre/article/article2
                |https://manage.thegulocal.com/help-centre/article/article3
                |https://manage.thegulocal.com/help-centre/article/how-can-i-redirect-my-delivery""".stripMargin
            )
          )
        }
      }
    }

    test("takeDownArticle") {
      val takeDown = takeDownArticle()("can-i-read-your-papermagazines-online")
      test("Number of files modified") {
        takeDown.map(_.length) ==> Right(3)
      }
      test("Article is removed from topics") {
        takeDown.map(_(0)) ==> Right(
          PathAndContent(
            "testTopics/apps",
            """{"path":"apps","title":"The Guardian apps","articles":[{"path":"how-can-i-redirect-my-delivery","title":"How can I redirect my delivery?"},{"path":"id-like-to-make-a-complaint-about-an-advertisement","title":"I'd like to make a complaint about an advertisement"},{"path":"im-unable-to-comment-and-need-help","title":"I'm unable to comment and need help"}]}"""
          )
        )
      }
      test("Article is removed from More topics") {
        takeDown.map(_(1)) ==> Right(
          PathAndContent(
            "testTopics/more-topics",
            """{"path":"more-topics","title":"More topics","topics":[{"path":"archives","title":"Back issues and archives","articles":[{"path":"b1","title":"Finding articles from the past in digital format"},{"path":"b2","title":"Old newspapers in physical format"}]},{"path":"events","title":"Events","articles":[{"path":"e1","title":"I can no longer attend the live online event, can I have a refund?"},{"path":"e2","title":"I can’t find my original confirmation email, can you resend me the event link?"},{"path":"e3","title":"Once I have purchased a ticket, how will I attend the online event?"},{"path":"e4","title":"I purchased a book with my ticket, when will I receive this?"}]},{"path":"gifting","title":"Gifting","articles":[{"path":"g1","title":"Gifting a Digital Subscription"}]},{"path":"newsletters-and-emails","title":"Newsletters and emails","articles":[{"path":"n1","title":"I'm not receiving any emails from you but think I should be"},{"path":"n2","title":"Manage your email preferences"}]},{"path":"the-guardian-apps","title":"The Guardian apps","articles":[{"path":"a2","title":"Apple/Google subscriptions"},{"path":"a3","title":"Personalising your apps"},{"path":"a1","title":"Premium tier access"}]}]}"""
          )
        )
      }
      test("Article is deleted") {
        takeDown.map(_(2)) ==> Right(PathAndContent("testArticles/can-i-read-your-papermagazines-online", ""))
      }
    }
  }
}
