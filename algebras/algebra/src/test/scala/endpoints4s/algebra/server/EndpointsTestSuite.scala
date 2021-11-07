package endpoints4s.algebra.server

import java.time.LocalDate
import java.util.UUID

import akka.http.scaladsl.model.HttpMethods.{DELETE, PUT, GET}
import akka.http.scaladsl.model.headers.{
  ETag,
  RawHeader,
  `Access-Control-Allow-Origin`,
  `Last-Modified`
}
import akka.http.scaladsl.model.{DateTime, HttpMethods, HttpRequest, StatusCodes}
import endpoints4s.{Invalid, Valid}

trait EndpointsTestSuite[T <: endpoints4s.algebra.EndpointsTestApi] extends ServerTestBase[T] {

  import DecodedUrl._
  import serverApi.{segment => s, _}

  "paths" should {

    "static" in {
      decodeUrl(path)("/") shouldEqual Matched(())
      decodeUrl(path)("/foo") shouldEqual NotMatched
      decodeUrl(path / "foo")("/foo") shouldEqual Matched(())
      decodeUrl(path / "foo" / "")("/foo/") shouldEqual Matched(())
      decodeUrl(path / "foo")("/") shouldEqual NotMatched
      decodeUrl(path / "foo")("/foo/") shouldEqual NotMatched
      decodeUrl(path / "foo" / "")("/foo") shouldEqual NotMatched
      decodeUrl(path / "foo" / "bar")("/foo/bar") shouldEqual Matched(())
      decodeUrl(path / "foo" / "bar")("/foo") shouldEqual NotMatched
    }

    "decode segments" in {
      decodeUrl(path / s[Int]())("/42") shouldEqual Matched(42)
      decodeUrl(path / s[Long]())("/42") shouldEqual Matched(42L)
      decodeUrl(path / s[Double]())("/42.0") shouldEqual Matched(42.0)
      decodeUrl(path / s[Int]() / "")("/42/") shouldEqual Matched(42)
      decodeUrl(path / s[Int]() / "")("/42") shouldEqual NotMatched
      decodeUrl(path / s[Int]())("/") shouldEqual NotMatched
      decodeUrl(path / s[Int]())("/42/bar") shouldEqual NotMatched
      decodeUrl(path / s[Int]())("/foo") shouldEqual Malformed(
        Seq(
          "Invalid integer value 'foo' for segment"
        )
      )
      decodeUrl(path / s[String]())("/foo%20bar") shouldEqual Matched("foo bar")
      decodeUrl(path / s[String]())("/foo/bar") shouldEqual NotMatched
      decodeUrl(path / s[Int]() / "baz")("/42/baz") shouldEqual Matched(42)
      decodeUrl(path / s[Int]() / "baz")("/foo/baz") shouldEqual Malformed(
        Seq(
          "Invalid integer value 'foo' for segment"
        )
      )
      decodeUrl(path / s[Int]() / "baz")("/42") shouldEqual NotMatched
      decodeUrl(path / s[Int]() / "baz")("/foo") shouldEqual NotMatched
      decodeUrl(path / "foo" / remainingSegments())("/foo/bar%2Fbaz/quux") shouldEqual Matched(
        "bar%2Fbaz/quux"
      )
      decodeUrl(path / "foo" / remainingSegments())("/foo") shouldEqual NotMatched
    }

    "transformed" in {
      val itemId = s[String]("itemId").xmapPartial { rawId =>
        val sep = rawId.indexOf("-")
        if (sep == -1)
          Invalid(s"Invalid item id value '$rawId' for segment 'itemId'")
        else {
          val (id, name) = rawId.splitAt(sep)
          Valid(Item(name.drop(1), id))
        }
      }(item => s"${item.id}-${item.name}")
      decodeUrl(path / itemId)("/42-programming-in-scala") shouldEqual Matched(
        Item("programming-in-scala", "42")
      )
      decodeUrl(path / itemId)("/foo") shouldEqual Malformed(
        Seq(
          "Invalid item id value 'foo' for segment 'itemId'"
        )
      )

      val file = s[String]("file").xmap(new java.io.File(_))(_.getPath)
      decodeUrl(path / "assets" / file)("/assets/favicon.png") shouldEqual Matched(
        new java.io.File("favicon.png")
      )
    }

  }

  "decode query strings" should {

    "primitives" in {
      decodeUrl(path / "foo" /? qs[Int]("n"))("/foo?n=42") shouldEqual Matched(
        42
      )
      decodeUrl(path / "foo" /? qs[Long]("n"))("/foo?n=42") shouldEqual Matched(
        42L
      )
      decodeUrl(path / "foo" /? qs[String]("s"))("/foo?s=bar") shouldEqual Matched(
        "bar"
      )
      decodeUrl(path / "foo" /? qs[String]("s"))("/foo?s=bar%2Cbaz") shouldEqual Matched(
        "bar,baz"
      )
      decodeUrl(path / "foo" /? qs[Boolean]("b"))("/foo?b=true") shouldEqual Matched(
        true
      )
      decodeUrl(path / "foo" /? qs[Boolean]("b"))("/foo?b=false") shouldEqual Matched(
        false
      )
      decodeUrl(path / "foo" / "" /? qs[Int]("n"))("/foo/?n=42") shouldEqual Matched(
        42
      )
      decodeUrl(path / "foo" /? qs[Int]("n"))("/foo") shouldEqual Malformed(
        Seq(
          "Missing value for query parameter 'n'"
        )
      )
      decodeUrl(path / "foo" /? qs[Int]("n"))("/foo?n=bar") shouldEqual Malformed(
        Seq(
          "Invalid integer value 'bar' for query parameter 'n'"
        )
      )
    }

    "optional" in {
      val url = path /? qs[Option[Int]]("n")
      decodeUrl(url)("/") shouldEqual Matched(None)
      decodeUrl(url)("/?n=42") shouldEqual Matched(Some(42))
      decodeUrl(url)("/?n=bar") shouldEqual Malformed(
        Seq(
          "Invalid integer value 'bar' for query parameter 'n'"
        )
      )
    }

    "optional with default value" in {
      val url = path /? optQsWithDefault[Int]("n", 42)
      decodeUrl(url)("/") shouldEqual Matched(42)
      decodeUrl(url)("/?n=42") shouldEqual Matched(42)
      decodeUrl(url)("/?n=43") shouldEqual Matched(43)
      decodeUrl(url)("/?n=bar") shouldEqual Malformed(
        Seq(
          "Invalid integer value 'bar' for query parameter 'n'"
        )
      )
    }

    "list" in {
      val url = path /? qs[List[Int]]("xs")
      decodeUrl(url)("/") shouldEqual Matched(Nil)
      decodeUrl(url)("/?xs=1&xs=2") shouldEqual Matched(1 :: 2 :: Nil)
      decodeUrl(url)("/?xs=1&xs=two") shouldEqual Malformed(
        Seq(
          "Invalid integer value 'two' for query parameter 'xs'"
        )
      )
      decodeUrl(url)("/?xs=one&xs=two") shouldEqual Malformed(
        Seq(
          "Invalid integer value 'one' for query parameter 'xs'",
          "Invalid integer value 'two' for query parameter 'xs'"
        )
      )
    }

    "transformed" in {
      implicit val pageQueryString: QueryStringParam[Page] =
        intQueryString.xmap(Page(_))(_.number)
      val url = path /? qs[Page]("page")
      decodeUrl(url)("/?page=42") shouldEqual Matched(Page(42))
      decodeUrl(url)("/?page=foo") shouldEqual Malformed(
        Seq(
          "Invalid integer value 'foo' for query parameter 'page'"
        )
      )

      implicit val blogSlugQueryString: QueryStringParam[BlogSlug] =
        stringQueryString.xmap(BlogSlug(_))(_.slug)
      val url2 = path /? qs[BlogSlug]("slug")
      decodeUrl(url2)("/?slug=this-is-a-slug") shouldEqual (Matched(
        BlogSlug("this-is-a-slug")
      ))
      decodeUrl(url2)("/?slug=this%20is%20a%20slug") shouldEqual (Matched(
        BlogSlug("this is a slug")
      ))
    }

  }

  "urls" should {

    "transformed" in {
      val paginatedUrl =
        (path /? (qs[Int]("from") & qs[Int]("limit")))
          .xmap((Page2.apply _).tupled)(p => (p.from, p.limit))
      decodeUrl(paginatedUrl)("/?from=1&limit=10") shouldEqual Matched(
        Page2(1, 10)
      )
      decodeUrl(paginatedUrl)("/?from=one&limit=10") shouldEqual Malformed(
        Seq(
          "Invalid integer value 'one' for query parameter 'from'"
        )
      )
      decodeUrl(paginatedUrl)("/?from=one&limit=ten") shouldEqual Malformed(
        Seq(
          "Invalid integer value 'one' for query parameter 'from'",
          "Invalid integer value 'ten' for query parameter 'limit'"
        )
      )
    }

  }

  "xmap query strings" should {

    "xmap urls of locations" in {

      val locationQueryString =
        (qs[Double]("lon") & qs[Double]("lat"))
          .xmap[Location] { case (lon, lat) =>
            Location(lon, lat)
          } { location => (location.longitude, location.latitude) }
      val locationUrl = path /? locationQueryString

      decodeUrl(locationUrl)("/?lon=12.0&lat=32.9") shouldEqual Matched(
        Location(12.0, 32.9)
      )
      decodeUrl(locationUrl)("/?lon=-12.0&lat=32.9") shouldEqual Matched(
        Location(-12.0, 32.9)
      )
      decodeUrl(locationUrl)(s"/?lon=${Math.PI}&lat=-32.9") shouldEqual Matched(
        Location(Math.PI, -32.9)
      )
      decodeUrl(locationUrl)("/?lon=12&lat=32") shouldEqual Matched(
        Location(12, 32)
      )
      decodeUrl(locationUrl)("/?lat=32.0&lon=12.0") shouldEqual Matched(
        Location(12.0, 32.0)
      )

      decodeUrl(locationUrl)("/?lon=12,0&lat=32.0") shouldEqual Malformed(
        Seq(
          "Invalid number value '12,0' for query parameter 'lon'"
        )
      )
      decodeUrl(locationUrl)("/?lon=a&lat=32") shouldEqual Malformed(
        Seq(
          "Invalid number value 'a' for query parameter 'lon'"
        )
      )
      decodeUrl(locationUrl)("/?let=12.0&lat=32.0") shouldEqual Malformed(
        Seq(
          "Missing value for query parameter 'lon'"
        )
      )
      decodeUrl(locationUrl)("/?lon=12.0") shouldEqual Malformed(
        Seq(
          "Missing value for query parameter 'lat'"
        )
      )
    }

    "xmapPartial urls of blogids" in {
      val blogIdQueryString: QueryString[BlogId] =
        (qs[Option[UUID]]("uuid") & qs[Option[String]]("slug"))
          .xmapPartial[BlogId] {
            case (Some(uuid), _)    => Valid(BlogUuid(uuid))
            case (None, Some(slug)) => Valid(BlogSlug(slug))
            case (None, None) =>
              Invalid("Missing either query parameter 'uuid' or 'slug'")
          } {
            case BlogUuid(uuid) => (Some(uuid), None)
            case BlogSlug(slug) => (None, Some(slug))
          }

      val testUUID: UUID = UUID.randomUUID()
      val testMalformedUUID1: String = "f4b9defa-1ad8-453f-9a06268d"
      val testMalformedUUID2: String = "f4b9defa-1ad8-453f-9o06-2683b8564b8d"
      val testSlug: String = "test-slug"

      decodeUrl(path /? blogIdQueryString)(s"/?uuid=$testUUID") shouldEqual Matched(
        BlogUuid(testUUID)
      )
      decodeUrl(path /? blogIdQueryString)(s"/?slug=$testSlug") shouldEqual Matched(
        BlogSlug(testSlug)
      )
      decodeUrl(path /? blogIdQueryString)(s"/?uuid=$testUUID&slug=$testSlug") shouldEqual Matched(
        BlogUuid(testUUID)
      )
      decodeUrl(path /? blogIdQueryString)(s"/?slug=$testSlug&uuid=$testUUID") shouldEqual Matched(
        BlogUuid(testUUID)
      )
      decodeUrl(path /? blogIdQueryString)(s"/?slug=") shouldEqual Matched(
        BlogSlug("")
      )

      decodeUrl(path /? blogIdQueryString)(s"/?uuid=$testMalformedUUID1") shouldEqual Malformed(
        Seq(
          s"Invalid UUID value '$testMalformedUUID1' for query parameter 'uuid'"
        )
      )
      decodeUrl(path /? blogIdQueryString)(s"/?uuid=$testMalformedUUID2") shouldEqual Malformed(
        Seq(
          s"Invalid UUID value '$testMalformedUUID2' for query parameter 'uuid'"
        )
      )
      decodeUrl(path /? blogIdQueryString)(s"/") shouldEqual Malformed(
        Seq(
          "Missing either query parameter 'uuid' or 'slug'"
        )
      )
    }

  }

  "multiple errors" should {
    "be accumulated" in {
      decodeUrl(path / s[Int]() /? qs[Int]("x"))("/foo?x=bar") shouldEqual Malformed(
        Seq(
          "Invalid integer value 'foo' for segment",
          "Invalid integer value 'bar' for query parameter 'x'"
        )
      )
    }
  }

  "Server interpreter" should {

    "return server response for UUID" in {

      val uuid = UUID.randomUUID()
      val mockedResponse = "interpretedServerResponse"

      serveEndpoint(serverApi.UUIDEndpoint, mockedResponse) { port =>
        val request =
          HttpRequest(uri = s"http://localhost:$port/user/$uuid/description?name=name1&age=18")
        whenReady(sendAndDecodeEntityAsText(request)) { case (response, entity) =>
          assert(entity == mockedResponse)
          assert(response.status.intValue() == 200)
          ()
        }
      }

      serveEndpoint(serverApi.putUUIDEndpoint, ()) { port =>
        val request =
          HttpRequest(method = PUT, uri = s"http://localhost:$port/user/$uuid")
        whenReady(send(request)) { case (response, entity) =>
          assert(entity.isEmpty)
          assert(response.status.intValue() == 200)
          ()
        }
      }

      serveEndpoint(serverApi.deleteUUIDEndpoint, ()) { port =>
        val request = HttpRequest(
          method = DELETE,
          uri = s"http://localhost:$port/user/$uuid"
        )
        whenReady(send(request)) { case (response, entity) =>
          assert(entity.isEmpty)
          assert(response.status.intValue() == 200)
          ()
        }
      }
    }

    "return server response" in {

      val mockedResponse = "interpretedServerResponse"

      serveEndpoint(serverApi.smokeEndpoint, mockedResponse) { port =>
        val request =
          HttpRequest(uri = s"http://localhost:$port/user/userId/description?name=name1&age=18")
        whenReady(sendAndDecodeEntityAsText(request)) { case (response, entity) =>
          assert(entity == mockedResponse)
          assert(response.status.intValue() == 200)
          ()
        }
      }

      serveEndpoint(serverApi.putEndpoint, ()) { port =>
        val request =
          HttpRequest(method = PUT, uri = s"http://localhost:$port/user/foo123")
        whenReady(send(request)) { case (response, entity) =>
          assert(entity.isEmpty)
          assert(response.status.intValue() == 200)
          ()
        }
      }

      serveEndpoint(serverApi.deleteEndpoint, ()) { port =>
        val request =
          HttpRequest(method = DELETE, s"http://localhost:$port/user/foo123")
        whenReady(send(request)) { case (response, entity) =>
          assert(entity.isEmpty)
          assert(response.status.intValue() == 200)
          ()
        }
      }

      serveEndpoint(serverApi.trailingSlashEndpoint, ()) { port =>
        val request = HttpRequest(method = GET, s"http://localhost:$port/user/")
        whenReady(send(request)) { case (response, entity) =>
          assert(entity.isEmpty)
          assert(response.status.intValue() == 200)
          ()
        }
      }
    }

    "Handle exceptions by default" in {
      serveEndpoint(serverApi.smokeEndpoint, sys.error("Sorry.")) { port =>
        val request = HttpRequest(uri = s"http://localhost:$port/user/foo/description?name=a&age=1")
        whenReady(sendAndDecodeEntityAsText(request)) { case (response, entity) =>
          assert(response.status.intValue() == 500)
          assert(entity == "[\"Sorry.\"]")
          ()
        }
      }
    }

    "encode response headers" in {
      val entity = "foo"
      val etag = UUID.randomUUID().toString
      val lastModified = DateTime.now
      val cache =
        serverApi.Cache(s""""$etag"""", lastModified.toRfc1123DateTimeString())
      serveEndpoint(serverApi.versionedResource, (entity, cache)) { port =>
        val request =
          HttpRequest(uri = s"http://localhost:$port/versioned-resource")
        whenReady(sendAndDecodeEntityAsText(request)) { case (response, responseEntity) =>
          assert(responseEntity == entity)
          assert(response.header[ETag].contains(ETag(etag)))
          assert(
            response
              .header[`Last-Modified`]
              .contains(`Last-Modified`(lastModified))
          )
          ()
        }
      }
    }

    "encode optional response headers" in {
      val entity = "foo"
      val origin = Some("*")
      serveEndpoint(
        serverApi.endpointWithOptionalResponseHeader,
        (entity, origin)
      ) { port =>
        val request =
          HttpRequest(uri = s"http://localhost:$port/maybe-cors-enabled")
        whenReady(sendAndDecodeEntityAsText(request)) { case (response, responseEntity) =>
          assert(responseEntity == entity)
          assert(
            response
              .header[`Access-Control-Allow-Origin`]
              .contains(`Access-Control-Allow-Origin`.*)
          )
          ()
        }
      }
    }

    "skip missing optional response headers" in {
      val entity = "foo"
      serveEndpoint(
        serverApi.endpointWithOptionalResponseHeader,
        (entity, None)
      ) { port =>
        val request =
          HttpRequest(uri = s"http://localhost:$port/maybe-cors-enabled")
        whenReady(sendAndDecodeEntityAsText(request)) { case (response, responseEntity) =>
          assert(responseEntity == entity)
          assert(response.header[`Access-Control-Allow-Origin`].isEmpty)
          ()
        }
      }
    }

    "reject requests with two missing headers" in {
      serveEndpoint(joinedHeadersEndpoint, "ignored") { port =>
        val noHeadersRequest =
          HttpRequest(uri = s"http://localhost:$port/joinedHeadersEndpoint")
        whenReady(sendAndDecodeEntityAsText(noHeadersRequest)) { case (response, entity) =>
          assert(response.status == StatusCodes.BadRequest)
          assert(entity == """["Missing header A","Missing header B"]""")
        }
        ()
      }
    }

    "reject requests with one missing header" in {
      serveEndpoint(joinedHeadersEndpoint, "ignored") { port =>
        val oneHeader =
          HttpRequest(uri = s"http://localhost:$port/joinedHeadersEndpoint")
            .withHeaders(RawHeader("A", "foo"))
        whenReady(sendAndDecodeEntityAsText(oneHeader)) { case (response, entity) =>
          assert(response.status == StatusCodes.BadRequest)
          assert(entity == """["Missing header B"]""")
        }
        ()
      }
    }

    "accept requests with the required headers" in {
      serveEndpoint(joinedHeadersEndpoint, "success") { port =>
        val twoHeaders =
          HttpRequest(uri = s"http://localhost:$port/joinedHeadersEndpoint")
            .withHeaders(RawHeader("A", "foo"), RawHeader("B", "foo"))
        whenReady(sendAndDecodeEntityAsText(twoHeaders)) { case (response, entity) =>
          assert(response.status == StatusCodes.OK)
          assert(entity == "success")
        }
        ()
      }
    }

    "accept requests with the required case insensitive headers" in {
      serveEndpoint(joinedHeadersEndpoint, "success") { port =>
        val twoHeaders =
          HttpRequest(uri = s"http://localhost:$port/joinedHeadersEndpoint")
            .withHeaders(RawHeader("a", "foo"), RawHeader("b", "foo"))
        whenReady(sendAndDecodeEntityAsText(twoHeaders)) { case (response, entity) =>
          assert(response.status == StatusCodes.OK)
          assert(entity == "success")
        }
        ()
      }
    }

    "decode transformed request headers" in {
      serveEndpoint(xmapHeadersEndpoint, "ignored") { port =>
        val validRequest =
          HttpRequest(uri = s"http://localhost:$port/xmapHeadersEndpoint")
            .withHeaders(RawHeader("C", "42"))
        whenReady(sendAndDecodeEntityAsText(validRequest)) { case (response, _) =>
          assert(response.status == StatusCodes.OK)
        }
        val invalidRequest =
          HttpRequest(uri = s"http://localhost:$port/xmapHeadersEndpoint")
            .withHeaders(RawHeader("C", "forty-two"))
        whenReady(sendAndDecodeEntityAsText(invalidRequest)) { case (response, entity) =>
          assert(response.status == StatusCodes.BadRequest)
          assert(entity == """["Invalid integer: forty-two"]""")
        }
        ()
      }
    }

    "decode transformed request entities" in {
      serveEndpoint(xmapReqBodyEndpoint, "ignored") { port =>
        val validRequest =
          HttpRequest(
            uri = s"http://localhost:$port/xmapReqBodyEndpoint",
            method = HttpMethods.POST
          ).withEntity(LocalDate.now().format(dateTimeFormatter))
        whenReady(sendAndDecodeEntityAsText(validRequest)) { case (response, _) =>
          assert(response.status == StatusCodes.OK)
        }
        val invalidRequest =
          HttpRequest(
            uri = s"http://localhost:$port/xmapReqBodyEndpoint",
            method = HttpMethods.POST
          ).withEntity("not a date")
        whenReady(sendAndDecodeEntityAsText(invalidRequest)) { case (response, entity) =>
          assert(response.status == StatusCodes.BadRequest)
          assert(entity == """["Invalid date value 'not a date'"]""")
        }
        ()
      }
    }

    "decode transformed request" in {
      serveEndpoint(endpointWithTransformedRequest, ()) { port =>
        val validRequest =
          HttpRequest(uri = s"http://localhost:$port/transformed-request?n=9")
            .withHeaders(RawHeader("Accept", "text/html"))
        whenReady(sendAndDecodeEntityAsText(validRequest)) { case (response, _) =>
          assert(response.status == StatusCodes.OK)
        }
        val invalidRequest =
          HttpRequest(uri = s"http://localhost:$port/transformed-request?n=10")
            .withHeaders(RawHeader("Accept", "text/html"))
        whenReady(sendAndDecodeEntityAsText(invalidRequest)) { case (response, entity) =>
          assert(response.status == StatusCodes.BadRequest)
          assert(
            entity == """["Invalid combination of request header and query string parameter"]"""
          )
        }
        ()
      }
    }

    "encode transformed response entities" in {
      val entity = StringWrapper("foo")
      serveEndpoint(
        serverApi.endpointWithTransformedResponseEntity,
        entity
      ) { port =>
        val request =
          HttpRequest(uri = s"http://localhost:$port/transformed-response-entity")
        whenReady(sendAndDecodeEntityAsText(request)) { case (_, responseEntity) =>
          assert(responseEntity == entity.str)
        }
        ()
      }
    }

    "encode transformed response" in {
      val resp = TransformedResponse("foo", "\"42\"")
      serveEndpoint(
        serverApi.endpointWithTransformedResponse,
        resp
      ) { port =>
        val request =
          HttpRequest(uri = s"http://localhost:$port/transformed-response")
        whenReady(sendAndDecodeEntityAsText(request)) { case (response, responseEntity) =>
          assert(responseEntity == resp.entity)
          assert(response.headers[ETag].contains(ETag("42")))
        }
        ()
      }
    }

  }
}

case class Item(name: String, id: String)
case class Page(number: Int)
case class Page2(from: Int, limit: Int)
case class Location(longitude: Double, latitude: Double)
sealed trait BlogId
case class BlogUuid(uuid: UUID) extends BlogId
case class BlogSlug(slug: String) extends BlogId
