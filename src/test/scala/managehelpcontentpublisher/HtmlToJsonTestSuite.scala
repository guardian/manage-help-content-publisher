package managehelpcontentpublisher

import utest._

/** See [[https://github.com/guardian/salesforce/blob/master/force-app/main/default/classes/ArticleBodyValidation.cls#L5-L20 list of HTML elements we support in Salesforce]].
  */
object HtmlToJsonTestSuite extends TestSuite {
  val tests: Tests = Tests {

    test("Element h2") {
      HtmlToJson("<h2>some subheading</h2>").render(indent = 2) ==> """[
                    |  {
                    |    "element": "h2",
                    |    "content": [
                    |      {
                    |        "element": "text",
                    |        "content": "some subheading"
                    |      }
                    |    ]
                    |  }
                    |]""".stripMargin
    }

    test("Element p") {
      HtmlToJson("<p>This is a sentence.</p>").render(indent = 2) ==> """[
                    |  {
                    |    "element": "p",
                    |    "content": [
                    |      {
                    |        "element": "text",
                    |        "content": "This is a sentence."
                    |      }
                    |    ]
                    |  }
                    |]""".stripMargin
    }

    test("Element ol") {
      HtmlToJson("""<ol>
                   |  <li>First</li>
                   |  <li>Second</li>
                   |  <li>Third</li>
                   |</ol>
                   |""".stripMargin)
        .render(indent = 2) ==> """[
                                        |  {
                                        |    "element": "ol",
                                        |    "content": [
                                        |      {
                                        |        "element": "li",
                                        |        "content": [
                                        |          {
                                        |            "element": "text",
                                        |            "content": "First"
                                        |          }
                                        |        ]
                                        |      },
                                        |      {
                                        |        "element": "li",
                                        |        "content": [
                                        |          {
                                        |            "element": "text",
                                        |            "content": "Second"
                                        |          }
                                        |        ]
                                        |      },
                                        |      {
                                        |        "element": "li",
                                        |        "content": [
                                        |          {
                                        |            "element": "text",
                                        |            "content": "Third"
                                        |          }
                                        |        ]
                                        |      }
                                        |    ]
                                        |  }
                                        |]""".stripMargin
    }

    test("Element ul") {
      HtmlToJson("""<ul>
                   |  <li>Something</li>
                   |  <li>Another thing</li>
                   |  <li>Something else</li>
                   |</ul>
                   |""".stripMargin)
        .render(indent = 2) ==> """[
                                        |  {
                                        |    "element": "ul",
                                        |    "content": [
                                        |      {
                                        |        "element": "li",
                                        |        "content": [
                                        |          {
                                        |            "element": "text",
                                        |            "content": "Something"
                                        |          }
                                        |        ]
                                        |      },
                                        |      {
                                        |        "element": "li",
                                        |        "content": [
                                        |          {
                                        |            "element": "text",
                                        |            "content": "Another thing"
                                        |          }
                                        |        ]
                                        |      },
                                        |      {
                                        |        "element": "li",
                                        |        "content": [
                                        |          {
                                        |            "element": "text",
                                        |            "content": "Something else"
                                        |          }
                                        |        ]
                                        |      }
                                        |    ]
                                        |  }
                                        |]""".stripMargin
    }

    test("Element br") {
      test("inside middle of paragraph") {
        HtmlToJson("<p>Breaking the paragraph here.<br>And starting again.</p>")
          .render(indent = 2) ==> """[
                                            |  {
                                            |    "element": "p",
                                            |    "content": [
                                            |      {
                                            |        "element": "text",
                                            |        "content": "Breaking the paragraph here."
                                            |      }
                                            |    ]
                                            |  },
                                            |  {
                                            |    "element": "p",
                                            |    "content": [
                                            |      {
                                            |        "element": "text",
                                            |        "content": "And starting again."
                                            |      }
                                            |    ]
                                            |  }
                                            |]""".stripMargin
      }

      test("after non-para text") {
        HtmlToJson("Breaking the paragraph here.<br><p>And starting again.</p>")
          .render(indent = 2) ==> """[
                                            |  {
                                            |    "element": "p",
                                            |    "content": [
                                            |      {
                                            |        "element": "text",
                                            |        "content": "Breaking the paragraph here."
                                            |      }
                                            |    ]
                                            |  },
                                            |  {
                                            |    "element": "p",
                                            |    "content": [
                                            |      {
                                            |        "element": "text",
                                            |        "content": "And starting again."
                                            |      }
                                            |    ]
                                            |  }
                                            |]""".stripMargin
      }

      test("before non-para text") {
        HtmlToJson("<p>Breaking the paragraph here.</p><br>And starting again.")
          .render(indent = 2) ==> """[
                                            |  {
                                            |    "element": "p",
                                            |    "content": [
                                            |      {
                                            |        "element": "text",
                                            |        "content": "Breaking the paragraph here."
                                            |      }
                                            |    ]
                                            |  },
                                            |  {
                                            |    "element": "p",
                                            |    "content": [
                                            |      {
                                            |        "element": "text",
                                            |        "content": "And starting again."
                                            |      }
                                            |    ]
                                            |  }
                                            |]""".stripMargin
      }

      test("between non-para text") {
        HtmlToJson("Breaking the paragraph here.<br>And starting again.")
          .render(indent = 2) ==> """[
                                            |  {
                                            |    "element": "p",
                                            |    "content": [
                                            |      {
                                            |        "element": "text",
                                            |        "content": "Breaking the paragraph here."
                                            |      }
                                            |    ]
                                            |  },
                                            |  {
                                            |    "element": "p",
                                            |    "content": [
                                            |      {
                                            |        "element": "text",
                                            |        "content": "And starting again."
                                            |      }
                                            |    ]
                                            |  }
                                            |]""".stripMargin
      }
      test("immediately after opening p tag") {
        HtmlToJson("<p><br>Please note that will need to give at least 3 days notice.</p>")
          .render(indent = 2) ==> """[
                                            |  {
                                            |    "element": "p",
                                            |    "content": [
                                            |      {
                                            |        "element": "text",
                                            |        "content": "Please note that will need to give at least 3 days notice."
                                            |      }
                                            |    ]
                                            |  }
                                            |]""".stripMargin
      }
      test("in middle of paragraph containing a link") {
        HtmlToJson("""<p>xyz<br />abc<a href="https://support.theguardian.com/">click here</a>def</p>""")
          .render(indent = 2) ==>
          """|[
             |  {
             |    "element": "p",
             |    "content": [
             |      {
             |        "element": "text",
             |        "content": "xyz"
             |      }
             |    ]
             |  },
             |  {
             |    "element": "p",
             |    "content": [
             |      {
             |        "element": "text",
             |        "content": "abc"
             |      },
             |      {
             |        "element": "a",
             |        "content": [
             |          {
             |            "element": "text",
             |            "content": "click here"
             |          }
             |        ],
             |        "href": "https://support.theguardian.com/"
             |      },
             |      {
             |        "element": "text",
             |        "content": "def"
             |      }
             |    ]
             |  }
             |]""".stripMargin
      }
    }

    test("Element a") {
      HtmlToJson(
        """<p><a href="https://support.theguardian.com/uk/subscribe/digital" target="_blank">digital subscription</a></p>"""
      )
        .render(indent = 2) ==>
        """|[
           |  {
           |    "element": "p",
           |    "content": [
           |      {
           |        "element": "a",
           |        "content": [
           |          {
           |            "element": "text",
           |            "content": "digital subscription"
           |          }
           |        ],
           |        "href": "https://support.theguardian.com/uk/subscribe/digital"
           |      }
           |    ]
           |  }
           |]""".stripMargin
    }

    test("Element b") {
      HtmlToJson("<p>This is <b>very</b> important</p>")
        .render(indent = 2) ==> """[
                                        |  {
                                        |    "element": "p",
                                        |    "content": [
                                        |      {
                                        |        "element": "text",
                                        |        "content": "This is "
                                        |      },
                                        |      {
                                        |        "element": "b",
                                        |        "content": [
                                        |          {
                                        |            "element": "text",
                                        |            "content": "very"
                                        |          }
                                        |        ]
                                        |      },
                                        |      {
                                        |        "element": "text",
                                        |        "content": " important"
                                        |      }
                                        |    ]
                                        |  }
                                        |]""".stripMargin
    }

    test("Element strong") {
      HtmlToJson("<p>This is <strong>very</strong> important</p>")
        .render(indent = 2) ==> """[
                                        |  {
                                        |    "element": "p",
                                        |    "content": [
                                        |      {
                                        |        "element": "text",
                                        |        "content": "This is "
                                        |      },
                                        |      {
                                        |        "element": "b",
                                        |        "content": [
                                        |          {
                                        |            "element": "text",
                                        |            "content": "very"
                                        |          }
                                        |        ]
                                        |      },
                                        |      {
                                        |        "element": "text",
                                        |        "content": " important"
                                        |      }
                                        |    ]
                                        |  }
                                        |]""".stripMargin
    }

    test("Element i") {
      HtmlToJson("<p>Something <i>amazing</i> happened</p>")
        .render(indent = 2) ==> """[
                                        |  {
                                        |    "element": "p",
                                        |    "content": [
                                        |      {
                                        |        "element": "text",
                                        |        "content": "Something "
                                        |      },
                                        |      {
                                        |        "element": "i",
                                        |        "content": [
                                        |          {
                                        |            "element": "text",
                                        |            "content": "amazing"
                                        |          }
                                        |        ]
                                        |      },
                                        |      {
                                        |        "element": "text",
                                        |        "content": " happened"
                                        |      }
                                        |    ]
                                        |  }
                                        |]""".stripMargin
    }

    test("Element em") {
      HtmlToJson("<p>Something <em>amazing</em> happened</p>")
        .render(indent = 2) ==> """[
                                        |  {
                                        |    "element": "p",
                                        |    "content": [
                                        |      {
                                        |        "element": "text",
                                        |        "content": "Something "
                                        |      },
                                        |      {
                                        |        "element": "i",
                                        |        "content": [
                                        |          {
                                        |            "element": "text",
                                        |            "content": "amazing"
                                        |          }
                                        |        ]
                                        |      },
                                        |      {
                                        |        "element": "text",
                                        |        "content": " happened"
                                        |      }
                                        |    ]
                                        |  }
                                        |]""".stripMargin
    }

    test("A heading with styling") {
      HtmlToJson("""<h2 style="text-align: center;">some text</h2>""")
        .render(indent = 2) ==> """[
                                        |  {
                                        |    "element": "h2",
                                        |    "content": [
                                        |      {
                                        |        "element": "text",
                                        |        "content": "some text"
                                        |      }
                                        |    ]
                                        |  }
                                        |]""".stripMargin
    }

    test("An article with multiple elements and inline links") {
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
          |        "element": "b",
          |        "content": [
          |          {
          |            "element": "text",
          |            "content": "Digital subscribers"
          |          }
          |        ]
          |      },
          |      {
          |        "element": "text",
          |        "content": " can log in on up to 10 devices."
          |      }
          |    ]
          |  },
          |  {
          |    "element": "p",
          |    "content": [
          |      {
          |        "element": "b",
          |        "content": [
          |          {
          |            "element": "text",
          |            "content": "Apple/Google subscribers"
          |          }
          |        ]
          |      },
          |      {
          |        "element": "text",
          |        "content": " are only able to log in on the device they subscribed with. If you’d like to use your subscription on more than one device we’d recommend exploring a "
          |      },
          |      {
          |        "element": "a",
          |        "content": [
          |          {
          |            "element": "text",
          |            "content": "digital subscription"
          |          }
          |        ],
          |        "href": "https://support.theguardian.com/uk/subscribe/digital"
          |      },
          |      {
          |        "element": "text",
          |        "content": ", which gives you premium tier access on up to 10 devices, along with access to the Guardian and Observer Daily Edition."
          |      }
          |    ]
          |  },
          |  {
          |    "element": "p",
          |    "content": [
          |      {
          |        "element": "text",
          |        "content": "If you have an account that is not associated with a digital or Apple/Google subscription, then you can log in on as many devices as you wish."
          |      }
          |    ]
          |  }
          |]""".stripMargin
      HtmlToJson(input).render(indent = 2) ==> expected
    }
  }
}
