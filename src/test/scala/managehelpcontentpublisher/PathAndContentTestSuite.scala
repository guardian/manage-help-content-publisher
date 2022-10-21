package managehelpcontentpublisher

import utest._

import java.net.URI
import scala.io.Source

/** See
  * [[https://github.com/guardian/salesforce/blob/master/force-app/main/default/classes/ArticleBodyValidation.cls#L5-L20 list of HTML elements we support in Salesforce]].
  */
object PathAndContentTestSuite extends TestSuite {

  def jsonFileToString(path: String): String = Source.fromResource(s"$path.json").mkString

  object Fixtures {

    def resource(kind: String, id: String): (String, String) = id -> Source.fromResource(s"${kind}s/$id.json").mkString
    def article(id: String): (String, String) = resource("article", id)
    def topic(id: String): (String, String) = resource("topic", id)

    val article1: (String, String) = article("how-can-i-redirect-my-delivery")
    val article2: (String, String) = article("can-i-read-your-papermagazines-online")
    val article3: (String, String) = article("changing-my-contribution-amount")
    val deliveryTopic: (String, String) = topic("delivery")
    val appsTopic: (String, String) = topic("apps")
    val moreTopics: (String, String) = topic("more-topics")
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
      articles: Map[String, String] = Map(Fixtures.article1, Fixtures.article2, Fixtures.article3),
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
        val json = jsonFileToString("api_input/input1")

        val published = publishContents()(json)
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
                |https://manage.thegulocal.com/help-centre/article/can-i-read-your-papermagazines-online
                |https://manage.thegulocal.com/help-centre/article/changing-my-contribution-amount""".stripMargin
            )
          )
        }
      }

      test("Newly published article") {
        val published = publishContents()(jsonFileToString("api_input/input2"))

        test("number of files published") {
          published.map(_.length) ==> Right(6)
        }
        test("article published") {
          published.map(_(0)) ==> Right(
            PathAndContent(
              "testArticles/how-to-stay-signed-in",
              """{"title":"How to stay signed in","body":[{"element":"p","content":[{"element":"text","content":"When you are signing into the Guardian website, we place a cookie in your browser that should recognise you when you return. This will not happen if your browser is set up in such a way as to block or delete cookies, although this will affect all of the sites that you visit. If you are getting this across multiple sites, the settings to check in your browser are:"}]},{"element":"p","content":[{"element":"text","content":"1. Whether you are running your browser in a private or incognito mode (this happens fairly regularly for people on Safari on iOS)."}]},{"element":"p","content":[{"element":"text","content":"2. What the cookie settings in your browser are (accepting cookies from sites you visit should cover this)."}]},{"element":"p","content":[{"element":"text","content":"3. Whether you are running any browser extensions or add-ons that block or clear cookies to protect your privacy."}]},{"element":"p","content":[{"element":"text","content":"If it is only the Guardian that you are having trouble with it may be that your sign in cookie has become corrupted. We would suggest that you try clearing any Guardian cookies from your browser and signing back in."}]},{"element":"p","content":[{"element":"text","content":"If the above doesn’t work, or if you would like more detailed instructions on how to do any of the above, please get in touch using the contact details at the bottom of this page and note the name of the browser that you are using."}]}],"path":"how-to-stay-signed-in","topics":[{"path":"accounts","title":"Accounts and sign in"},{"path":"apps","title":"The Guardian apps"},{"path":"website","title":"The Guardian website"}]}"""
            )
          )
        }
        test("accounts topic published") {
          published.map(_(1)) ==> Right(
            PathAndContent(
              "testTopics/accounts",
              """{"path":"accounts","title":"Accounts and sign in","articles":[{"path":"how-to-sign-in-using-a-computer","title":"How to sign in using a computer"},{"path":"how-to-sign-in-using-a-mobile-tablet","title":"How to sign in using a mobile/tablet"},{"path":"how-to-stay-signed-in","title":"How to stay signed in"},{"path":"i-need-help-signing-in","title":"I need help signing in"},{"path":"i-need-to-change-my-contact-details","title":"I need to change my contact details"},{"path":"i-want-to-delete-my-account","title":"I want to delete my account"},{"path":"i-ve-forgotten-my-password","title":"I've forgotten my password"},{"path":"i-need-to-update-my-email-preferences","title":"Manage your email preferences"},{"path":"signing-in-on-multiple-devices","title":"Signing in on multiple devices"},{"path":"what-does-signing-in-mean-for-my-data","title":"What does signing in mean for my data?"}]}"""
            )
          )
        }
        test("app topic published") {
          published.map(_(2)) ==> Right(
            PathAndContent(
              "testTopics/apps",
              """{"path":"apps","title":"The Guardian apps","articles":[{"path":"what-devices-are-compatible-with-your-apps","title":"Device compatibility"},{"path":"getting-started-with-your-digital-subscription","title":"Getting started with your Digital Subscription"},{"path":"i-have-a-googleitunes-subscription-that-i-need-help-with","title":"Google/Apple subscriptions"},{"path":"how-can-i-get-full-access-to-the-app-as-a-contributor","title":"How can I get full access to the app?"},{"path":"how-to-sign-in-using-a-mobile-tablet","title":"How to sign in using a mobile/tablet"},{"path":"how-to-stay-signed-in","title":"How to stay signed in"},{"path":"i-need-help-signing-in","title":"I need help signing in"},{"path":"making-your-app-more-personal","title":"Personalising your app"},{"path":"how-can-i-gain-access-to-the-premium-tier-of-your-app","title":"Premium Tier Access"},{"path":"why-am-i-still-seeing-adsbanners","title":"Why am I still seeing ads/banners?"}]}"""
            )
          )
        }
        test("website topic published") {
          published.map(_(3)) ==> Right(
            PathAndContent(
              "testTopics/website",
              """{"path":"website","title":"The Guardian website","articles":[{"path":"can-i-read-your-papermagazines-online","title":"Can I read your paper/magazines online?"},{"path":"how-to-stay-signed-in","title":"How to stay signed in"},{"path":"id-like-to-make-a-complaint-about-an-advertisement","title":"I'd like to make a complaint about an advertisement"},{"path":"im-unable-to-comment-and-need-help","title":"I'm unable to comment and need help"},{"path":"ive-found-a-bug-how-can-i-report-it","title":"I’ve found a bug, how can I report it?"},{"path":"why-am-i-still-seeing-adsbanners","title":"Why am I still seeing ads/banners?"},{"path":"why-have-i-been-banned-from-commenting","title":"Why have I been banned from commenting?"}]}"""
            )
          )
        }
        test("sitemap updated") {
          published.map(_(5)) ==> Right(
            PathAndContent(
              "DEV/sitemap.txt",
              """https://manage.thegulocal.com/help-centre/article/article1
                |https://manage.thegulocal.com/help-centre/article/article2
                |https://manage.thegulocal.com/help-centre/article/article3
                |https://manage.thegulocal.com/help-centre/article/changing-my-contribution-amount
                |https://manage.thegulocal.com/help-centre/article/how-to-stay-signed-in""".stripMargin
            )
          )
        }
      }

      test("When article has a non-core topic and 'More topics' is empty") {
        val published = publishContents()(jsonFileToString("api_input/input3"))
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
                |https://manage.thegulocal.com/help-centre/article/can-i-read-your-papermagazines-online
                |https://manage.thegulocal.com/help-centre/article/changing-my-contribution-amount""".stripMargin
            )
          )
        }
      }

      test("Article, topic and more topics when article has a non-core topic and 'More topics' is not empty") {
        val published = publishContents(
          previousArticles = Map(Fixtures.article2),
          previousTopics = Map(Fixtures.moreTopics)
        )(jsonFileToString("api_input/input4"))
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
              """{"path":"more-topics","title":"More topics","topics":[{"path":"archives","title":"Back issues and archives","articles":[{"path":"can-i-read-your-papermagazines-online","title":"Can I read your paper/magazines online?"},{"path":"id-like-to-make-a-complaint-about-an-advertisement","title":"I'd like to make a complaint about an advertisement"},{"path":"im-unable-to-comment-and-need-help","title":"I'm unable to comment and need help"}]},{"path":"events","title":"Events","articles":[{"path":"e1","title":"I can no longer attend the live online event, can I have a refund?"},{"path":"e2","title":"I can’t find my original confirmation email, can you resend me the event link?"},{"path":"e3","title":"Once I have purchased a ticket, how will I attend the online event?"},{"path":"e4","title":"I purchased a book with my ticket, when will I receive this?"}]},{"path":"gifting","title":"Gifting","articles":[{"path":"g1","title":"Gifting a Digital Subscription"}]},{"path":"newsletters-and-emails","title":"Newsletters and emails","articles":[{"path":"n1","title":"I'm not receiving any emails from you but think I should be"},{"path":"n2","title":"Manage your email preferences"}]},{"path":"the-guardian-apps","title":"The Guardian apps","articles":[{"path":"a2","title":"Apple/Google subscriptions"},{"path":"a3","title":"Personalising your apps"},{"path":"a1","title":"Premium tier access"},{"path":"changing-my-contribution-amount","title":"Changing my contribution amount"}]}]}""".stripMargin
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
                |https://manage.thegulocal.com/help-centre/article/can-i-read-your-papermagazines-online
                |https://manage.thegulocal.com/help-centre/article/changing-my-contribution-amount""".stripMargin
            )
          )
        }
      }

      test("When topic has changed, publish article and new topic and remove article from old topic") {
        val published = publishContents(
          previousArticles = Map(Fixtures.article1),
          previousTopics = Map(Fixtures.deliveryTopic)
        )(jsonFileToString("api_input/input5"))
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
                |https://manage.thegulocal.com/help-centre/article/changing-my-contribution-amount
                |https://manage.thegulocal.com/help-centre/article/how-can-i-redirect-my-delivery""".stripMargin
            )
          )
        }
      }
    }

    test("takeDownArticle") {
      val takeDown = takeDownArticle()("changing-my-contribution-amount")
      test("Number of files modified") {
        takeDown.map(_.length) ==> Right(4)
      }
      test("Article is removed from topics") {
        takeDown.map(_(0)) ==> Right(
          PathAndContent(
            "testTopics/apps",
            """{"path":"apps","title":"The Guardian apps","articles":[{"path":"can-i-read-your-papermagazines-online","title":"Can I read your paper/magazines online?"},{"path":"how-can-i-redirect-my-delivery","title":"How can I redirect my delivery?"},{"path":"id-like-to-make-a-complaint-about-an-advertisement","title":"I'd like to make a complaint about an advertisement"},{"path":"im-unable-to-comment-and-need-help","title":"I'm unable to comment and need help"}]}"""
          )
        )
      }
      test("Article is removed from More topics") {
        takeDown.map(_(1)) ==> Right(
          PathAndContent(
            "testTopics/more-topics",
            """{"path":"more-topics","title":"More topics","topics":[{"path":"archives","title":"Back issues and archives","articles":[{"path":"b1","title":"Finding articles from the past in digital format"},{"path":"b2","title":"Old newspapers in physical format"}]},{"path":"events","title":"Events","articles":[{"path":"e1","title":"I can no longer attend the live online event, can I have a refund?"},{"path":"e2","title":"I can’t find my original confirmation email, can you resend me the event link?"},{"path":"e3","title":"Once I have purchased a ticket, how will I attend the online event?"},{"path":"e4","title":"I purchased a book with my ticket, when will I receive this?"}]},{"path":"gifting","title":"Gifting","articles":[{"path":"g1","title":"Gifting a Digital Subscription"}]},{"path":"newsletters-and-emails","title":"Newsletters and emails","articles":[{"path":"n1","title":"I'm not receiving any emails from you but think I should be"},{"path":"n2","title":"Manage your email preferences"}]},{"path":"the-guardian-apps","title":"The Guardian apps","articles":[{"path":"a2","title":"Apple/Google subscriptions"},{"path":"can-i-read-your-papermagazines-online","title":"Can I read your paper/magazines online?"},{"path":"a3","title":"Personalising your apps"},{"path":"a1","title":"Premium tier access"}]}]}"""
          )
        )
      }
      test("Article is deleted") {
        takeDown.map(_(2)) ==> Right(PathAndContent("testArticles/changing-my-contribution-amount", ""))
      }
      test("Article is removed from sitemap") {
        takeDown.map(_(3)) ==> Right(
          PathAndContent(
            "DEV/sitemap.txt",
            """https://manage.thegulocal.com/help-centre/article/article1
            |https://manage.thegulocal.com/help-centre/article/article2
            |https://manage.thegulocal.com/help-centre/article/article3""".stripMargin
          )
        )
      }
    }
  }
}
