package managehelpcontentpublisher

import managehelpcontentpublisher.Config.config
import utest._

/** See [[https://github.com/guardian/salesforce/blob/master/force-app/main/default/classes/ArticleBodyValidation.cls#L5-L20 list of HTML elements we support in Salesforce]].
  */
object PathAndContentTestSuite extends TestSuite {

  object Fixtures {

    val article: (String, String) = "how-can-i-redirect-my-delivery" ->
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
      |          "path": "a1",
      |          "title": "Premium tier access"
      |        },
      |        {
      |          "path": "a2",
      |          "title": "Apple/Google subscriptions"
      |        },
      |        {
      |          "path": "a3",
      |          "title": "Personalising your apps"
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
  }

  private def publishContents(
      previousArticles: Map[String, String] = Map(),
      previousTopics: Map[String, String] = Map()
  ) =
    PathAndContent.publishContents(
      fetchArticleByPath = { article => Right(previousArticles.get(article)) },
      fetchTopicByPath = { topic => Right(previousTopics.get(topic)) },
      storeArticle = { article => Right(PathAndContent(s"testArticles/${article.path}", article.content)) },
      storeTopic = { topic => Right(PathAndContent(s"testTopics/${topic.path}", topic.content)) }
    ) _

  val tests: Tests = Tests {

    test("Publish when article has a core topic") {
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
        published.map(_.length) ==> Right(2)
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
    }

    test("Publish when article has a non-core topic and 'More topics' is empty") {
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
        published.map(_.length) ==> Right(3)
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
    }

    test("Publish article, topic and more topics when article has a non-core topic and 'More topics' is not empty") {
      val published = publishContents(previousTopics = Map(Fixtures.moreTopics))(
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
        published.map(_.length) ==> Right(3)
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
            """{"path":"more-topics","title":"More topics","topics":[{"path":"archives","title":"Back issues and archives","articles":[{"path":"can-i-read-your-papermagazines-online","title":"Can I read your paper/magazines online?"},{"path":"id-like-to-make-a-complaint-about-an-advertisement","title":"I'd like to make a complaint about an advertisement"},{"path":"im-unable-to-comment-and-need-help","title":"I'm unable to comment and need help"}]},{"path":"events","title":"Events","articles":[{"path":"e1","title":"I can no longer attend the live online event, can I have a refund?"},{"path":"e2","title":"I can’t find my original confirmation email, can you resend me the event link?"},{"path":"e3","title":"Once I have purchased a ticket, how will I attend the online event?"},{"path":"e4","title":"I purchased a book with my ticket, when will I receive this?"}]},{"path":"gifting","title":"Gifting","articles":[{"path":"g1","title":"Gifting a Digital Subscription"}]},{"path":"newsletters-and-emails","title":"Newsletters and emails","articles":[{"path":"n1","title":"I'm not receiving any emails from you but think I should be"},{"path":"n2","title":"Manage your email preferences"}]},{"path":"the-guardian-apps","title":"The Guardian apps","articles":[{"path":"a1","title":"Premium tier access"},{"path":"a2","title":"Apple/Google subscriptions"},{"path":"a3","title":"Personalising your apps"}]}]}"""
          )
        )
      }
    }

    test("When topic has changed, publish article and new topic and remove article from old topic") {
      val published = publishContents(
        previousArticles = Map(Fixtures.article),
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
        published.map(_.length) ==> Right(3)
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
    }
  }
}
