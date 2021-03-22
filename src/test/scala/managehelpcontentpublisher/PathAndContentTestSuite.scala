package managehelpcontentpublisher

import utest._

/** See [[https://github.com/guardian/salesforce/blob/master/force-app/main/default/classes/ArticleBodyValidation.cls#L5-L20 list of HTML elements we support in Salesforce]].
  */
object PathAndContentTestSuite extends TestSuite {

  val tests: Tests = Tests {

    test("Publish expected content") {
      val publishContents = PathAndContent.publishContents(
        { article => Right(PathAndContent(s"testArticles/${article.path}", article.content)) },
        { topic => Right(PathAndContent(s"testTopics/${topic.path}", topic.content)) }
      ) _
      publishContents("""{
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
                        |}""".stripMargin) ==> Right(
        Seq(
          PathAndContent(
            "testArticles/can-i-read-your-papermagazines-online",
            """{"title":"Can I read your paper/magazines online?","body":[{"element":"p","content":[{"element":"text","content":"We do not"}]}],"path":"can-i-read-your-papermagazines-online","topics":[{"path":"website","title":"The Guardian website"}]}"""
          ),
          PathAndContent(
            "testTopics/website",
            """{"path":"website","title":"The Guardian website","articles":[{"path":"can-i-read-your-papermagazines-online","title":"Can I read your paper/magazines online?"},{"path":"id-like-to-make-a-complaint-about-an-advertisement","title":"I'd like to make a complaint about an advertisement"},{"path":"im-unable-to-comment-and-need-help","title":"I'm unable to comment and need help"}]}"""
          )
        )
      )
    }
  }
}
