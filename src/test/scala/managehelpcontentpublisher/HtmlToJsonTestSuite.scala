package managehelpcontentpublisher

import utest._

object HtmlToJsonTestSuite extends TestSuite {
  val tests: Tests = Tests {
    test("Works with an article with multiple elements and inline links") {
      val input = """<p><strong>Digital subscribers</strong> can log in on up to 10 devices.</p>
                |
                |<p><strong>Apple/Google subscribers</strong> are only able to log in on the device they subscribed with. If you&rsquo;d like to use your subscription on more than one device we&rsquo;d recommend exploring a <a href="https://support.theguardian.com/uk/subscribe/digital" target="_blank">digital subscription</a>, which gives you premium tier access on up to 10 devices, along with access to the Guardian and Observer Daily Edition.</p>
                |
                |<p>If you have an account that is not associated with a digital or Apple/Google subscription, then you can log in on as many devices as you wish.</p>
                |""".stripMargin
      val expected =
        """[
          |  {
          |    "element": "p",
          |    "content": [
          |      {
          |        "element": "strong",
          |        "content": [
          |          {
          |            "text": "Digital subscribers"
          |          }
          |        ]
          |      },
          |      {
          |        "text": " can log in on up to 10 devices."
          |      }
          |    ]
          |  },
          |  {
          |    "text": " "
          |  },
          |  {
          |    "element": "p",
          |    "content": [
          |      {
          |        "element": "strong",
          |        "content": [
          |          {
          |            "text": "Apple/Google subscribers"
          |          }
          |        ]
          |      },
          |      {
          |        "text": " are only able to log in on the device they subscribed with. If you’d like to use your subscription on more than one device we’d recommend exploring a "
          |      },
          |      {
          |        "element": "a",
          |        "content": [
          |          {
          |            "text": "digital subscription"
          |          }
          |        ],
          |        "href": "https://support.theguardian.com/uk/subscribe/digital",
          |        "target": "_blank"
          |      },
          |      {
          |        "text": ", which gives you premium tier access on up to 10 devices, along with access to the Guardian and Observer Daily Edition."
          |      }
          |    ]
          |  },
          |  {
          |    "text": " "
          |  },
          |  {
          |    "element": "p",
          |    "content": [
          |      {
          |        "text": "If you have an account that is not associated with a digital or Apple/Google subscription, then you can log in on as many devices as you wish."
          |      }
          |    ]
          |  }
          |]""".stripMargin
      HtmlToJson(input).render(indent = 2) ==> expected
    }
  }
}
