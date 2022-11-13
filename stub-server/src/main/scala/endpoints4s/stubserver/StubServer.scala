package endpoints4s.stubserver

import java.io.InputStream
import java.security.KeyStore
import java.security.SecureRandom
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext

import akka.actor.ActorSystem
import akka.http.scaladsl.ConnectionContext
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.Framing
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import scala.util.Try

/** In development:
  * sbt "~stub-server/reStart"
  */
object StubServer extends App {

  val conf = ConfigFactory
    .parseString("akka.http.server.preview.enable-http2 = on")
    .withFallback(ConfigFactory.defaultApplication())

  implicit val actorSystem: ActorSystem = ActorSystem("StubServer", conf)
  implicit val executionContext: ExecutionContextExecutor = actorSystem.dispatcher

  val requestHandler: HttpRequest => Future[HttpResponse] = {
    case HttpRequest(
          GET,
          uri,
          _,
          _,
          _
        )
        if uri.toRelative == Uri(
          "/user/f3ac9be0-6339-4650-afb6-7305ece8edce/description?name=name1&age=18"
        ) =>
      HttpResponse(entity = "wiremockeResponse")
    case HttpRequest(
          GET,
          uri,
          _,
          _,
          _
        ) if uri.toRelative == Uri("/user/userId/description?name=name1&age=18") =>
      HttpResponse(entity = "wiremockeResponse")
    case HttpRequest(
          GET,
          uri,
          _,
          _,
          _
        )
        if uri.toRelative == Uri("/user/userId/whatever?id=1bdae951-63ee-46b9-8ff0-4976acb8d48e") =>
      HttpResponse(entity = "wiremockeResponse")
    case HttpRequest(
          GET,
          uri,
          _,
          _,
          _
        )
        if uri.toRelative == Uri(
          "/user/userId/whatever?id=1bdae951-63ee-46b9-8ff0-4976acb8d48e&age=18"
        ) =>
      HttpResponse(entity = "wiremockeResponse")
    case HttpRequest(
          GET,
          uri,
          _,
          _,
          _
        )
        if uri.toRelative == Uri(
          "/user/userId/whatever?name=name1"
        ) =>
      HttpResponse(entity = "wiremockeResponse")
    case HttpRequest(
          GET,
          uri,
          _,
          _,
          _
        )
        if uri.toRelative == Uri(
          "/user/userId/whatever?name=name1&age=18"
        ) =>
      HttpResponse(entity = "wiremockeResponse")
    case HttpRequest(
          GET,
          uri,
          _,
          _,
          _
        )
        if uri.toRelative == Uri(
          "/error/user/userId/description?name=name1&age=18"
        ) =>
      HttpResponse(StatusCodes.NotImplemented)
    case HttpRequest(
          GET,
          uri,
          _,
          _,
          _
        )
        if uri.toRelative == Uri(
          "/detailed/error/user/userId/description?name=name1&age=18"
        ) =>
      HttpResponse(StatusCodes.InternalServerError, entity = "[\"Unable to process your request\"]")
    case HttpRequest(
          GET,
          uri,
          headers,
          _,
          _
        )
        if uri.toRelative == Uri(
          "/joinedHeadersEndpoint"
        ) && headers.find(_.is("a")).exists(_.value == "a") && headers
          .find(_.is("b"))
          .exists(_.value == "b") =>
      HttpResponse(entity = "29d15495-55ea-431e-bef3-392b05b14fef")
    case HttpRequest(
          GET,
          uri,
          headers,
          _,
          _
        )
        if uri.toRelative == Uri(
          "/xmapHeadersEndpoint"
        ) && headers.find(_.is("c")).exists(_.value == "11") =>
      HttpResponse(entity = "f2ed5a13-9113-4717-9b21-65cd72a5540e")
    case HttpRequest(
          GET,
          uri,
          _,
          _,
          _
        ) if uri.toRelative == Uri("/xmapUrlEndpoint/11") =>
      HttpResponse(entity = "f4e4ccbf-710a-4b38-bf8b-a9eb0a92382c")
    case r @ HttpRequest(
          POST,
          uri,
          _,
          requestEntity,
          _
        ) if uri.toRelative == Uri("/xmapReqBodyEndpoint") =>
      Unmarshal(requestEntity)
        .to[String]
        .flatMap {
          case value if value == "2018-04-14" =>
            HttpResponse(entity = "dbb2297e-ae8c-4413-aab3-978833794c79")
          case _ =>
            matcherExhausted(r)
        }
    case HttpRequest(
          GET,
          uri,
          _,
          _,
          _
        ) if uri.toRelative == Uri("/users/1") =>
      HttpResponse(entity = "wiremockeResponse")
    case HttpRequest(
          GET,
          uri,
          _,
          _,
          _
        ) if uri.toRelative == Uri("/not/found/users/1") =>
      HttpResponse(StatusCodes.NotFound, entity = "")
    case HttpRequest(
          GET,
          uri,
          _,
          _,
          _
        ) if uri.toRelative == Uri("/user/") =>
      HttpResponse()
    case HttpRequest(
          GET,
          uri,
          _,
          _,
          _
        ) if uri.toRelative == Uri("/versioned-resource") =>
      HttpResponse(
        entity = "foo",
        headers = Seq(
          `Access-Control-Expose-Headers`("ETag"),
          ETag("d88b0456-67cb-40e5-8f0a-7664f3e93348"),
          `Last-Modified`(DateTime(2021, 1, 1, 12, 30))
        )
      )
    case HttpRequest(
          GET,
          uri,
          _,
          _,
          _
        ) if uri.toRelative == Uri("/optional-response-header/some") =>
      HttpResponse(
        entity = "foo",
        headers = Seq(
          `Access-Control-Expose-Headers`("A"), {
            final class A(value: String) extends ModeledCustomHeader[A] {
              override def renderInRequests = true
              override def renderInResponses = true
              override val companion = A
              override def value: String = value
            }
            object A extends ModeledCustomHeaderCompanion[A] {
              override val name = "A"
              override def parse(value: String) = Try(new A(value))
            }
            A("a")
          }
        )
      )
    case HttpRequest(
          GET,
          uri,
          _,
          _,
          _
        ) if uri.toRelative == Uri("/optional-response-header/none") =>
      HttpResponse(entity = "foo")
    case HttpRequest(
          GET,
          uri,
          _,
          _,
          _
        ) if uri.toRelative == Uri("/basic-auth/success") =>
      HttpResponse(entity = "wiremockeResponse")
    case HttpRequest(
          GET,
          uri,
          _,
          _,
          _
        ) if uri.toRelative == Uri("/basic-auth/failure") =>
      HttpResponse(StatusCodes.Forbidden)
    case r @ HttpRequest(
          POST,
          uri,
          _,
          requestEntity,
          _
        ) if uri.toRelative == Uri("/user-json-codec") =>
      Unmarshal(requestEntity)
        .to[String]
        .flatMap {
          case value if value == "{\"name\":\"name2\",\"age\":19}" =>
            HttpResponse(entity = "{\"street\":\"avenue1\",\"city\":\"NY\"}")
          case _ =>
            matcherExhausted(r)
        }
    case r @ HttpRequest(
          POST,
          uri,
          _,
          requestEntity,
          _
        ) if uri.toRelative == Uri("/user") =>
      Unmarshal(requestEntity)
        .to[String]
        .flatMap {
          case value if value == "{\"name\":\"name2\",\"age\":19}" =>
            HttpResponse(entity = "{\"street\":\"avenue1\",\"city\":\"NY\"}")
          case _ =>
            matcherExhausted(r)
        }
    case HttpRequest(
          POST,
          uri,
          _,
          requestEntity,
          _
        )
        if uri.toRelative == Uri(
          "/text"
        ) && requestEntity.contentType == ContentTypes.`text/plain(UTF-8)` =>
      HttpResponse(entity = "Oekraïene")
    case HttpRequest(
          POST,
          uri,
          _,
          requestEntity,
          _
        )
        if uri.toRelative == Uri(
          "/user-or-name"
        ) && (requestEntity.contentType == ContentTypes.`application/json` || requestEntity.contentType == ContentTypes.`text/plain(UTF-8)`) =>
      HttpResponse()
    case HttpRequest(
          GET,
          uri,
          _,
          _,
          _
        ) if uri.toRelative == Uri("/bytes") =>
      HttpResponse(entity =
        HttpEntity.Chunked.fromData(
          ContentTypes.`application/octet-stream`,
          Source(
            List(
              ByteString(0.toByte),
              ByteString(1.toByte),
              ByteString(2.toByte)
            )
          )
        )
      )
    case HttpRequest(
          GET,
          uri,
          _,
          _,
          _
        ) if uri.toRelative == Uri("/text") =>
      HttpResponse(entity =
        HttpEntity.Chunked.fromData(
          ContentTypes.`text/plain(UTF-8)`,
          Source(
            List(
              ByteString("a"),
              ByteString("a"),
              ByteString("a"),
              ByteString("b"),
              ByteString("b"),
              ByteString("b"),
              ByteString("c"),
              ByteString("c"),
              ByteString("c")
            )
          )
        )
      )
    case r @ HttpRequest(
          POST,
          Uri.Path("/counter-values"),
          _,
          requestEntity,
          _
        ) =>
      requestEntity.dataBytes
        .via(
          Framing.delimiter(
            ByteString("\n"),
            maximumFrameLength = Int.MaxValue,
            allowTruncation = true
          )
        )
        .runWith(Sink.seq)
        .map { result =>
          if (
            result == Seq(
              ByteString("{\"value\":1}"),
              ByteString("{\"value\":2}"),
              ByteString("{\"value\":3}")
            )
          ) {
            HttpResponse()
          } else {
            matcherExhausted(r)
          }
        }
    case HttpRequest(
          GET,
          uri,
          _,
          _,
          _
        ) if uri.toRelative == Uri("/notifications") =>
      HttpResponse(entity =
        HttpEntity.Chunked.fromData(
          ContentTypes.`application/json`,
          Source(
            List(
              ByteString("{\"value\":1}"),
              ByteString("{\"value\":2}"),
              ByteString("{\"value\":3}")
            )
          ).intersperse(ByteString("\n"))
        )
      )
    case HttpRequest(
          GET,
          uri,
          _,
          _,
          _
        ) if uri.toRelative == Uri("/notifications/single") =>
      HttpResponse(entity =
        HttpEntity.Chunked.fromData(
          ContentTypes.`application/json`,
          Source(
            List(
              ByteString("{\"value\":1}")
            )
          ).intersperse(ByteString("\n"))
        )
      )
    case HttpRequest(
          GET,
          uri,
          _,
          _,
          _
        ) if uri.toRelative == Uri("/notifications/error") =>
      HttpResponse(entity =
        HttpEntity.Chunked.fromData(
          ContentTypes.`application/json`,
          Source(
            List(ByteString("{\"value\":1}"), ByteString("{\"value\":true}"))
          ).intersperse(ByteString("\n"))
        )
      )
    case HttpRequest(
          GET,
          uri,
          _,
          _,
          _
        ) if uri.toRelative == Uri("/notifications/empty") =>
      HttpResponse(entity =
        HttpEntity.Chunked.fromData(ContentTypes.`application/json`, Source(List.empty))
      )
    case r @ HttpRequest(
          POST,
          uri,
          _,
          requestEntity,
          _
        ) if uri.toRelative == Uri("/upload") =>
      requestEntity.dataBytes
        .mapConcat(_.toSeq)
        .runWith(Sink.seq)
        .map { result =>
          if (result == Seq[Byte](1, 2, 3, 4, 5, 6)) {
            HttpResponse()
          } else {
            matcherExhausted(r)
          }
        }
    case HttpRequest(
          GET,
          uri @ Uri.Path("/mapped-left"),
          headers,
          _,
          _
        )
        if uri.query().get("x").contains("1") && uri.query().get("y").contains("2") && headers
          .find(_.is("if-none-match"))
          .exists(_.value == "\"xxx\"") && headers
          .find(_.is("if-modified-since"))
          .exists(_.value == "Wed, 21 Oct 2015 07:28:00 GMT") =>
      HttpResponse(StatusCodes.NotModified)
    case HttpRequest(
          GET,
          uri @ Uri.Path("/mapped-right"),
          headers,
          _,
          _
        )
        if uri.query().get("x").contains("1") && uri.query().get("y").contains("2") && headers
          .find(_.is("if-none-match"))
          .exists(_.value == "foo") && headers
          .find(_.is("if-modified-since"))
          .exists(_.value == "bar") =>
      HttpResponse(
        headers = Seq(
          `Access-Control-Expose-Headers`("ETag"),
          ETag("xxx"),
          `Last-Modified`(DateTime(2015, 10, 21, 7, 28))
        )
      )
    case HttpRequest(
          GET,
          uri,
          _,
          _,
          _
        ) if uri.toRelative == Uri("/slow-response") =>
      Thread.sleep(10000)
      HttpResponse(entity = "It was slow!")
    case HttpRequest(
          GET,
          uri,
          _,
          _,
          _
        ) if Set(Uri("/"), Uri("")).contains(uri.toRelative) =>
      HttpResponse(entity = "StubServer running!")
    case r: HttpRequest =>
      matcherExhausted(r)
  }

  private def matcherExhausted(r: HttpRequest) = {
    val error = s"Request matcher exhausted for request [$r]."
    println(error)
    r.discardEntityBytes() // important to drain incoming HTTP Entity stream
    HttpResponse(404, entity = error)
  }

  implicit def toFuture[T](value: T): Future[T] = Future.successful(value)

  val exampleServerContext = {
    def resourceStream(resourceName: String): InputStream = {
      val is = getClass.getClassLoader.getResourceAsStream(resourceName)
      require(is ne null, s"Resource $resourceName not found")
      is
    }

    // never put passwords into code!
    val password = "abcdef".toCharArray

    val ks = KeyStore.getInstance("PKCS12")
    ks.load(resourceStream("keys/server.p12"), password)

    val keyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(ks, password)

    val context = SSLContext.getInstance("TLS")
    context.init(keyManagerFactory.getKeyManagers, null, new SecureRandom)

    ConnectionContext.httpsServer(context)
  }

  Http().newServerAt("0.0.0.0", 8080).bind(requestHandler).onComplete {
    case Success(binding) =>
      val address = binding.localAddress
      println(s"Stub server online at http://${address.getHostName}:${address.getPort}/")
    case Failure(ex) =>
      println(s"Failed to bind HTTP endpoint, terminating system ${ex.toString}")
  }

  Http()
    .newServerAt("0.0.0.0", 8081)
    .enableHttps(exampleServerContext)
    .bind(requestHandler)
    .onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        println(s"Stub server online at https://${address.getHostName}:${address.getPort}/")
      case Failure(ex) =>
        println(s"Failed to bind HTTPS endpoint, terminating system ${ex.toString}")
    }
}
