package endpoints.algebra.server

import java.nio.charset.Charset
import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods.{PUT, DELETE}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.model.headers.`Content-Type`
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.ByteString

import scala.concurrent.Future
import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration

trait EndpointsTestSuite[+T <: endpoints.algebra.EndpointsTestApi] extends ServerTestBase[T] {

  import serverApi.{ segment => s, _}
  import DecodedUrl._

  private implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val streamMaterializer: Materializer = ActorMaterializer()
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
        .getOrElse(Charset.forName("utf-8"))
    entity.decodeString(charset)
  }

  override protected def afterAll(): Unit = {
    Await.ready(actorSystem.terminate(), Duration.Inf)
    super.afterAll()
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
      decodeUrl(path / s[Int]())         ("/foo")       shouldEqual Malformed
      decodeUrl(path / s[String]())      ("/foo%20bar") shouldEqual Matched("foo bar")
      decodeUrl(path / s[String]())      ("/foo/bar")   shouldEqual NotMatched
      decodeUrl(path / s[Int]() / "baz") ("/42/baz")    shouldEqual Matched(42)
      decodeUrl(path / s[Int]() / "baz") ("/foo/baz")   shouldEqual Malformed
      decodeUrl(path / s[Int]() / "baz") ("/42")        shouldEqual NotMatched
//      decodeUrl(path / s[Int]() / "baz") ("/foo")       shouldEqual NotMatched
      decodeUrl(path / "foo" / remainingSegments()) ("/foo/bar%2Fbaz/quux") shouldEqual Matched("bar%2Fbaz/quux")
      decodeUrl(path / "foo" / remainingSegments()) ("/foo")                shouldEqual NotMatched
    }

    "transformed" in {
      val itemId = s[String]("itemId").xmapPartial { rawId =>
        val sep = rawId.indexOf("-")
        if (sep == -1) None else {
          val (id, name) = rawId.splitAt(sep)
          Some(Item(name.drop(1), id))
        }
      }(item => s"${item.id}-${item.name}")
      decodeUrl(path / itemId)("/42-programming-in-scala") shouldEqual Matched(Item("programming-in-scala", "42"))
      decodeUrl(path / itemId)("/foo")                     shouldEqual Malformed

      val file = s[String]("file").xmap[java.io.File](new java.io.File(_), _.getPath)
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
      decodeUrl(path / "foo" /? qs[Int]("n"))     ("/foo")         shouldEqual Malformed
      decodeUrl(path / "foo" /? qs[Int]("n"))     ("/foo?n=bar")   shouldEqual Malformed
    }

    "optional" in {
      val url = path /? qs[Option[Int]]("n")
      decodeUrl(url)("/")       shouldEqual Matched(None)
      decodeUrl(url)("/?n=42")  shouldEqual Matched(Some(42))
      decodeUrl(url)("/?n=bar") shouldEqual Malformed
    }

    "list" in {
      val url = path /? qs[List[Int]]("xs")
      decodeUrl(url)("/")             shouldEqual Matched(Nil)
      decodeUrl(url)("/?xs=1&xs=2")   shouldEqual Matched(1 :: 2 :: Nil)
      decodeUrl(url)("/?xs=1&xs=two") shouldEqual Malformed
    }

    "transformed" in {
      implicit val pageQueryString: QueryStringParam[Page] =
        intQueryString.xmap[Page](Page, _.number)
      val url = path /? qs[Page]("page")
      decodeUrl(url)("/?page=42")  shouldEqual Matched(Page(42))
      decodeUrl(url)("/?page=foo") shouldEqual Malformed
    }

  }

  "urls" should {

    "transformed" in {
      val paginatedUrl =
        (path /? (qs[Int]("from") & qs[Int]("limit")))
          .xmap[Page2](Page2.tupled, p => (p.from, p.limit))
      decodeUrl(paginatedUrl)("/?from=1&limit=10")   shouldEqual Matched(Page2(1, 10))
      decodeUrl(paginatedUrl)("/?from=one&limit=10") shouldEqual Malformed
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
  }

}

case class Item(name: String, id: String)
case class Page(number: Int)
case class Page2(from: Int, limit: Int)
