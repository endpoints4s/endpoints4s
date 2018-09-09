package endpoints.algebra.client

import java.time.LocalDate
import java.util.UUID

import com.github.tomakehurst.wiremock.client.WireMock._
import endpoints.algebra.EndpointsTestApi

trait EndpointsTestSuite[T <: EndpointsTestApi] extends ClientTestBase[T] {

  def clientTestSuite() = {

    "Client interpreter" should {

      "return server response" in {

        val response = "wiremockeResponse"

        wireMockServer.stubFor(get(urlEqualTo("/user/userId/description?name=name1&age=18"))
          .willReturn(aResponse()
            .withStatus(200)
            .withBody(response)))

        whenReady(call(client.smokeEndpoint, ("userId", "name1", 18))) {
          _ shouldEqual response
        }
        whenReady(call(client.emptyResponseSmokeEndpoint, ("userId", "name1", 18))) {
          _ shouldEqual (())
        }

      }


      "return correct url with optional parameter" in {

        val response = "wiremockeResponse"

        wireMockServer.stubFor(get(urlEqualTo("/user/userId/whatever?name=name1"))
          .willReturn(aResponse()
            .withStatus(200)
            .withBody(response)))

        wireMockServer.stubFor(get(urlEqualTo("/user/userId/whatever?name=name1&age=18"))
          .willReturn(aResponse()
            .withStatus(200)
            .withBody(response)))

        whenReady(call(client.optQsEndpoint, ("userId", "name1", None))) {
          _ shouldEqual response
        }
        whenReady(call(client.optQsEndpoint, ("userId", "name1", Some(18)))) {
          _ shouldEqual response
        }

      }

      "throw exception when 5xx is returned from server" in {
        wireMockServer.stubFor(get(urlEqualTo("/user/userId/description?name=name1&age=18"))
          .willReturn(aResponse()
            .withStatus(501)
            .withBody("")))

        whenReady(call(client.smokeEndpoint, ("userId", "name1", 18)).failed)(x => x.getMessage shouldBe "Unexpected status code: 501")
        whenReady(call(client.emptyResponseSmokeEndpoint, ("userId", "name1", 18)).failed)(x => x.getMessage shouldBe "Unexpected status code: 501")
      }

      "properly handle joined headers" in {
        val response = UUID.randomUUID().toString
        wireMockServer.stubFor(get(urlEqualTo("/joinedHeadersEndpoint"))
          .withHeader("A", equalTo("a"))
          .withHeader("B", equalTo("b"))
          .willReturn(aResponse()
            .withStatus(200)
            .withBody(response)))

        whenReady(call(client.joinedHeadersEndpoint, ("a", "b")))(x => x shouldEqual (response))
      }

      "properly handle xmaped headers" in {
        val response = UUID.randomUUID().toString
        wireMockServer.stubFor(get(urlEqualTo("/xmapHeadersEndpoint"))
          .withHeader("C", equalTo("11"))
          .willReturn(aResponse()
            .withStatus(200)
            .withBody(response)))

        whenReady(call(client.xmapHeadersEndpoint, 11))(x => x shouldEqual (response))
      }

      "properly handle xmaped url" in {
        val response = UUID.randomUUID().toString
        wireMockServer.stubFor(get(urlEqualTo("/xmapUrlEndpoint/11"))
          .willReturn(aResponse()
            .withStatus(200)
            .withBody(response)))

        whenReady(call(client.xmapUrlEndpoint, "11"))(x => x shouldEqual (response))
      }

      "properly handle xmaped request entites" in {
        val response = UUID.randomUUID().toString
        val dateString = "2018-04-14"
        val date = LocalDate.parse(dateString)
        wireMockServer.stubFor(post(urlEqualTo("/xmapReqBodyEndpoint"))
          .withRequestBody(equalTo(dateString))
          .willReturn(aResponse()
            .withStatus(200)
            .withBody(response)))

        whenReady(call(client.xmapReqBodyEndpoint, date))(x => x shouldEqual (response))
      }

      "in case of optional response" should {

        "return Some when response code is 2xx" in {

          val response = "wiremockeResponse"

          wireMockServer.stubFor(get(urlEqualTo("/users/1"))
            .willReturn(aResponse()
              .withStatus(200)
              .withBody(response)))

          whenReady(call(client.optionalEndpoint, ()))(_ shouldEqual Some(response))

        }

        "return None if server returned 404" in {

          wireMockServer.stubFor(get(urlEqualTo("/users/1"))
            .willReturn(aResponse()
              .withStatus(404)
              .withBody("")))

          whenReady(call(client.optionalEndpoint, ()))(_ shouldEqual None)

        }
      }

    }


  }


}
