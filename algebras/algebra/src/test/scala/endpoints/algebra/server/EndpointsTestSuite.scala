package endpoints.algebra.server

import java.nio.charset.StandardCharsets
import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.model.HttpMethods.{DELETE, PUT}
import akka.http.scaladsl.model.headers.`Content-Type`
import akka.util.ByteString
import endpoints.{Invalid, Valid}

import scala.concurrent.{ExecutionContext, Future}

trait EndpointsTestSuite[T <: endpoints.algebra.EndpointsTestApi] extends ServerTestBase[T] {

  import serverApi.{ segment => s, _}
  import DecodedUrl._

  private[server] implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val executionContext: ExecutionContext = actorSystem.dispatcher

  val httpClient = Http()

  def sendAndDecodeEntityAsText(request: HttpRequest): Future[(HttpResponse, String)] = {
    send(request).map { case (response, entity) =>
      (response, decodeEntityAsText(response, entity))
    }
  }

  def send(request: HttpRequest): Future[(HttpResponse, ByteString)] = {
    httpClient.singleRequest(request).flatMap { response =>
      response.entity.toStrict(patienceConfig.timeout).map { entity =>
        (response, entity.data)
      }
    }
  }

  def decodeEntityAsText(response: HttpResponse, entity: ByteString): String = {
    val charset =
      response.header[`Content-Type`]
        .flatMap(_.contentType.charsetOption.map(_.nioCharset))
        .getOrElse(StandardCharsets.UTF_8)
    entity.decodeString(charset)
  }

  "paths" should {

    "static" in {
      decodeUrl(path)                 ("/")        shouldEqual Matched(())
      decodeUrl(path)                 ("/foo")     shouldEqual NotMatched
      decodeUrl(path / "foo")         ("/foo")     shouldEqual Matched(())
      decodeUrl(path / "foo")         ("/")        shouldEqual NotMatched
      decodeUrl(path / "foo" / "bar") ("/foo/bar") shouldEqual Matched(())
      decodeUrl(path / "foo" / "bar") ("/foo")     shouldEqual NotMatched
    }

    "decode segments" in {
      decodeUrl(path / s[Int]())         ("/42")        shouldEqual Matched(42)
      decodeUrl(path / s[Long]())        ("/42")        shouldEqual Matched(42L)
      decodeUrl(path / s[Double]())      ("/42.0")      shouldEqual Matched(42.0)
      decodeUrl(path / s[Int]())         ("/")          shouldEqual NotMatched
      decodeUrl(path / s[Int]())         ("/42/bar")    shouldEqual NotMatched
      decodeUrl(path / s[Int]())         ("/foo")       shouldEqual Malformed(Seq(
        "Invalid integer value 'foo' for segment"
      ))
      decodeUrl(path / s[String]())      ("/foo%20bar") shouldEqual Matched("foo bar")
      decodeUrl(path / s[String]())      ("/foo/bar")   shouldEqual NotMatched
      decodeUrl(path / s[Int]() / "baz") ("/42/baz")    shouldEqual Matched(42)
      decodeUrl(path / s[Int]() / "baz") ("/foo/baz")   shouldEqual Malformed(Seq(
        "Invalid integer value 'foo' for segment"
      ))
      decodeUrl(path / s[Int]() / "baz") ("/42")        shouldEqual NotMatched
      decodeUrl(path / s[Int]() / "baz") ("/foo")       shouldEqual NotMatched
      decodeUrl(path / "foo" / remainingSegments()) ("/foo/bar%2Fbaz/quux") shouldEqual Matched("bar%2Fbaz/quux")
      decodeUrl(path / "foo" / remainingSegments()) ("/foo")                shouldEqual NotMatched
    }

    "transformed" in {
      val itemId = s[String]("itemId").xmapPartial { rawId =>
        val sep = rawId.indexOf("-")
        if (sep == -1) Invalid(s"Invalid item id value '$rawId' for segment 'itemId'") else {
          val (id, name) = rawId.splitAt(sep)
          Valid(Item(name.drop(1), id))
        }
      }(item => s"${item.id}-${item.name}")
      decodeUrl(path / itemId)("/42-programming-in-scala") shouldEqual Matched(Item("programming-in-scala", "42"))
      decodeUrl(path / itemId)("/foo")                     shouldEqual Malformed(Seq(
        "Invalid item id value 'foo' for segment 'itemId'"
      ))

      val file = s[String]("file").xmap(new java.io.File(_))(_.getPath)
      decodeUrl(path / "assets" / file)("/assets/favicon.png") shouldEqual Matched(new java.io.File("favicon.png"))
    }

  }

  "decode query strings" should {

    "primitives" in {
      decodeUrl(path / "foo" /? qs[Int]("n"))     ("/foo?n=42")    shouldEqual Matched(42)
      decodeUrl(path / "foo" /? qs[Long]("n"))    ("/foo?n=42")    shouldEqual Matched(42L)
      decodeUrl(path / "foo" /? qs[String]("s"))  ("/foo?s=bar")   shouldEqual Matched("bar")
      decodeUrl(path / "foo" /? qs[Boolean]("b")) ("/foo?b=true")  shouldEqual Matched(true)
      decodeUrl(path / "foo" /? qs[Boolean]("b")) ("/foo?b=false") shouldEqual Matched(false)
      decodeUrl(path / "foo" /? qs[Int]("n"))     ("/foo")         shouldEqual Malformed(Seq(
        "Missing value for query parameter 'n'"
      ))
      decodeUrl(path / "foo" /? qs[Int]("n"))     ("/foo?n=bar")   shouldEqual Malformed(Seq(
        "Invalid integer value 'bar' for query parameter 'n'"
      ))
    }

    "optional" in {
      val url = path /? qs[Option[Int]]("n")
      decodeUrl(url)("/")       shouldEqual Matched(None)
      decodeUrl(url)("/?n=42")  shouldEqual Matched(Some(42))
      decodeUrl(url)("/?n=bar") shouldEqual Malformed(Seq(
        "Invalid integer value 'bar' for query parameter 'n'"
      ))
    }

    "list" in {
      val url = path /? qs[List[Int]]("xs")
      decodeUrl(url)("/")             shouldEqual Matched(Nil)
      decodeUrl(url)("/?xs=1&xs=2")   shouldEqual Matched(1 :: 2 :: Nil)
      decodeUrl(url)("/?xs=1&xs=two") shouldEqual Malformed(Seq(
        "Invalid integer value 'two' for query parameter 'xs'"
      ))
      decodeUrl(url)("/?xs=one&xs=two") shouldEqual Malformed(Seq(
        "Invalid integer value 'one' for query parameter 'xs'",
        "Invalid integer value 'two' for query parameter 'xs'"
      ))
    }

    "transformed" in {
      implicit val pageQueryString: QueryStringParam[Page] =
        intQueryString.xmap(Page)(_.number)
      val url = path /? qs[Page]("page")
      decodeUrl(url)("/?page=42")  shouldEqual Matched(Page(42))
      decodeUrl(url)("/?page=foo") shouldEqual Malformed(Seq(
        "Invalid integer value 'foo' for query parameter 'page'"
      ))
    }

  }

  "urls" should {

    "transformed" in {
      val paginatedUrl =
        (path /? (qs[Int]("from") & qs[Int]("limit")))
          .xmap(Page2.tupled)(p => (p.from, p.limit))
      decodeUrl(paginatedUrl)("/?from=1&limit=10")    shouldEqual Matched(Page2(1, 10))
      decodeUrl(paginatedUrl)("/?from=one&limit=10")  shouldEqual Malformed(Seq(
        "Invalid integer value 'one' for query parameter 'from'"
      ))
      decodeUrl(paginatedUrl)("/?from=one&limit=ten") shouldEqual Malformed(Seq(
        "Invalid integer value 'one' for query parameter 'from'",
        "Invalid integer value 'ten' for query parameter 'limit'"
      ))
    }

  }

  "xmap query strings" should {

    "xmap urls of locations" in {

      val locationQueryString =
        (qs[Double]("lon") & qs[Double]("lat"))
          .xmap[Location] {
            case (lon, lat) => Location(lon, lat)
          } {
            location => (location.longitude, location.latitude)
          }
      val locationUrl = path /? locationQueryString

      decodeUrl(locationUrl)("/?lon=12.0&lat=32.9") shouldEqual Matched(Location(12.0, 32.9))
      decodeUrl(locationUrl)("/?lon=-12.0&lat=32.9") shouldEqual Matched(Location(-12.0, 32.9))
      decodeUrl(locationUrl)(s"/?lon=${Math.PI}&lat=-32.9") shouldEqual Matched(Location(Math.PI, -32.9))
      decodeUrl(locationUrl)("/?lon=12&lat=32") shouldEqual Matched(Location(12, 32))
      decodeUrl(locationUrl)("/?lat=32.0&lon=12.0") shouldEqual Matched(Location(12.0, 32.0))

      decodeUrl(locationUrl)("/?lon=12,0&lat=32.0") shouldEqual Malformed(Seq(
        "Invalid number value '12,0' for query parameter 'lon'"
      ))
      decodeUrl(locationUrl)("/?lon=a&lat=32") shouldEqual Malformed(Seq(
        "Invalid number value 'a' for query parameter 'lon'"
      ))
      decodeUrl(locationUrl)("/?let=12.0&lat=32.0") shouldEqual Malformed(Seq(
        "Missing value for query parameter 'lon'"
      ))
      decodeUrl(locationUrl)("/?lon=12.0") shouldEqual Malformed(Seq(
        "Missing value for query parameter 'lat'"
      ))
    }

    "xmapPartial urls of blogids" in {
      val blogIdQueryString: QueryString[BlogId] =
        (qs[Option[UUID]]("uuid") & qs[Option[String]]("slug"))
          .xmapPartial[BlogId] {
            case (Some(uuid), _)    => Valid(BlogUuid(uuid))
            case (None, Some(slug)) => Valid(BlogSlug(slug))
            case (None, None)       => Invalid("Missing either query parameter 'uuid' or 'slug'")
          } {
            case BlogUuid(uuid) => (Some(uuid), None)
            case BlogSlug(slug) => (None, Some(slug))
          }

      val testUUID: UUID = UUID.randomUUID()
      val testMalformedUUID1: String = "f4b9defa-1ad8-453f-9a06268d"
      val testMalformedUUID2: String = "f4b9defa-1ad8-453f-9o06-2683b8564b8d"
      val testSlug: String = "test-slug"

      decodeUrl(path /? blogIdQueryString)(s"/?uuid=$testUUID") shouldEqual Matched(BlogUuid(testUUID))
      decodeUrl(path /? blogIdQueryString)(s"/?slug=$testSlug") shouldEqual Matched(BlogSlug(testSlug))
      decodeUrl(path /? blogIdQueryString)(s"/?uuid=$testUUID&slug=$testSlug") shouldEqual Matched(BlogUuid(testUUID))
      decodeUrl(path /? blogIdQueryString)(s"/?slug=$testSlug&uuid=$testUUID") shouldEqual Matched(BlogUuid(testUUID))
      decodeUrl(path /? blogIdQueryString)(s"/?slug=") shouldEqual Matched(BlogSlug(""))

      decodeUrl(path /? blogIdQueryString)(s"/?uuid=$testMalformedUUID1") shouldEqual Malformed(Seq(
        s"Invalid UUID value '$testMalformedUUID1' for query parameter 'uuid'"
      ))
      decodeUrl(path /? blogIdQueryString)(s"/?uuid=$testMalformedUUID2") shouldEqual Malformed(Seq(
        s"Invalid UUID value '$testMalformedUUID2' for query parameter 'uuid'"
      ))
      decodeUrl(path /? blogIdQueryString)(s"/") shouldEqual Malformed(Seq(
        "Missing either query parameter 'uuid' or 'slug'"
      ))
    }

  }

  "multiple errors" should {
    "be accumulated" in {
      decodeUrl(path / s[Int]() /? qs[Int]("x"))("/foo?x=bar") shouldEqual Malformed(Seq(
        "Invalid integer value 'foo' for segment",
        "Invalid integer value 'bar' for query parameter 'x'"
      ))
    }
  }

  "Server interpreter" should {

    "return server response for UUID" in {

      val uuid = UUID.randomUUID()
      val mockedResponse = "interpretedServerResponse"

      serveEndpoint(serverApi.UUIDEndpoint, mockedResponse) { port =>
        val request = HttpRequest(uri = s"http://localhost:$port/user/$uuid/description?name=name1&age=18")
        whenReady(sendAndDecodeEntityAsText(request)) { case (response, entity) =>
          assert(entity == mockedResponse)
          assert(response.status.intValue == 200)
          ()
        }
      }

      serveEndpoint(serverApi.putUUIDEndpoint, ()) { port =>
        val request = HttpRequest(method = PUT, uri = s"http://localhost:$port/user/$uuid")
        whenReady(send(request)) { case (response, entity) =>
          assert(entity.isEmpty)
          assert(response.status.intValue == 200)
          ()
        }
      }

      serveEndpoint(serverApi.deleteUUIDEndpoint, ()) { port =>
        val request = HttpRequest(method = DELETE, uri = s"http://localhost:$port/user/$uuid")
        whenReady(send(request)) { case (response, entity) =>
          assert(entity.isEmpty)
          assert(response.status.intValue == 200)
          ()
        }
      }
    }

    "return server response" in {

      val mockedResponse = "interpretedServerResponse"

      serveEndpoint(serverApi.smokeEndpoint, mockedResponse) { port =>
        val request = HttpRequest(uri = s"http://localhost:$port/user/userId/description?name=name1&age=18")
        whenReady(sendAndDecodeEntityAsText(request)) { case (response, entity) =>
          assert(entity == mockedResponse)
          assert(response.status.intValue == 200)
          ()
        }
      }

      serveEndpoint(serverApi.putEndpoint, ()) { port =>
        val request = HttpRequest(method = PUT, uri = s"http://localhost:$port/user/foo123")
        whenReady(send(request)) { case (response, entity) =>
          assert(entity.isEmpty)
          assert(response.status.intValue == 200)
          ()
        }
      }

      serveEndpoint(serverApi.deleteEndpoint, ()) { port =>
        val request = HttpRequest(method = DELETE, s"http://localhost:$port/user/foo123")
        whenReady(send(request)) { case (response, entity) =>
          assert(entity.isEmpty)
          assert(response.status.intValue == 200)
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

  }
}

case class Item(name: String, id: String)
case class Page(number: Int)
case class Page2(from: Int, limit: Int)
case class Location(longitude: Double, latitude: Double)
sealed trait BlogId
case class BlogUuid(uuid: UUID) extends BlogId
case class BlogSlug(slug: String) extends BlogId
