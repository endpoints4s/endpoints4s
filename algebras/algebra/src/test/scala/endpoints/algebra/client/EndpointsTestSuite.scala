package endpoints.algebra.client

import java.time.LocalDate
import java.util.UUID

import com.github.tomakehurst.wiremock.client.WireMock._
import endpoints.algebra.EndpointsTestApi

trait EndpointsTestSuite[T <: EndpointsTestApi] extends ClientTestBase[T] {

  def clientTestSuite() = {

    "Client interpreter" should {

      "return server response for UUID" in {

        val uuid = UUID.randomUUID()
        val response = "wiremockeResponse"

        wireMockServer.stubFor(get(urlEqualTo(s"/user/$uuid/description?name=name1&age=18"))
          .willReturn(aResponse()
            .withStatus(200)
            .withBody(response)))

        whenReady(call(client.UUIDEndpoint, (uuid, "name1", 18))) {
          _ shouldEqual response
        }
        whenReady(call(client.emptyResponseUUIDEndpoint, (uuid, "name1", 18))) {
          _ shouldEqual (())
        }

      }

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

      "return correct url with optional UUID parameter" in {

        val uuid = UUID.randomUUID()
        val response = "wiremockeResponse"

        wireMockServer.stubFor(get(urlEqualTo(s"/user/userId/whatever?id=$uuid"))
          .willReturn(aResponse()
            .withStatus(200)
            .withBody(response)))

        wireMockServer.stubFor(get(urlEqualTo(s"/user/userId/whatever?id=$uuid&age=18"))
          .willReturn(aResponse()
            .withStatus(200)
            .withBody(response)))

        whenReady(call(client.optUUIDQsEndpoint, ("userId", uuid, None))) {
          _ shouldEqual response
        }
        whenReady(call(client.optUUIDQsEndpoint, ("userId", uuid, Some(18)))) {
          _ shouldEqual response
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

      "encode query strings" should {
        import client._

        "primitives" in {
          encodeUrl(path / "foo" /? qs[Int]("n"))(42) shouldEqual "/foo?n=42"
          encodeUrl(path / "foo" /? qs[Long]("n"))(42L) shouldEqual "/foo?n=42"
          encodeUrl(path / "foo" /? qs[String]("s"))("bar") shouldEqual "/foo?s=bar"
          encodeUrl(path / "foo" /? qs[Boolean]("b"))(true) shouldEqual "/foo?b=true"
          encodeUrl(path / "foo" /? qs[Double]("x"))(1.0) shouldEqual "/foo?x=1.0"
          encodeUrl(path / "foo" /? qs[UUID]("id"))(UUID.fromString("f4b9defa-1ad8-453f-9a06-2683b8564b8d")) shouldEqual "/foo?id=f4b9defa-1ad8-453f-9a06-2683b8564b8d"
        }

        "escaping" in {
          encodeUrl(path /? qs[String]("q"))("foo bar/baz") shouldEqual "?q=foo+bar%2Fbaz"
        }

        "multiple parameters" in {
          encodeUrl(path /? (qs[Int]("x") & qs[Int]("y")))((0, 1)) shouldEqual "?x=0&y=1"
        }

        "optional parameters" in {
          encodeUrl(path /? qs[Option[Int]]("n"))(Some(42)) shouldEqual "?n=42"
          encodeUrl(path /? qs[Option[Int]]("n"))(None) shouldEqual ""
          encodeUrl(path /? (qs[Option[Int]]("n") & qs[Int]("v")))((None, 42)) shouldEqual "?v=42"
          encodeUrl(path /? (qs[Option[Int]]("n") & qs[Int]("v")))((Some(0), 42)) shouldEqual "?n=0&v=42"
        }

        "list parameters" in {
          encodeUrl(path /? qs[List[Int]]("ids"))(1 :: 2 :: Nil) shouldEqual "?ids=1&ids=2"
          encodeUrl(path /? qs[List[Int]]("ids"))(Nil) shouldEqual ""
          encodeUrl(path /? (qs[List[Int]]("ids") & qs[Option[Int]]("x")))((Nil, None)) shouldEqual ""
          encodeUrl(path /? (qs[List[Int]]("ids") & qs[Option[Int]]("x")))((Nil, Some(0))) shouldEqual "?x=0"
          encodeUrl(path /? (qs[List[Int]]("ids") & qs[Option[Int]]("x")))((1 :: Nil, None)) shouldEqual "?ids=1"
        }

      }

    }


  }


}
