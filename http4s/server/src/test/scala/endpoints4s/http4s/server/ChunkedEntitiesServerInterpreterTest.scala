package endpoints4s.http4s.server

import java.net.ServerSocket

import endpoints4s.{Invalid, Valid}
import endpoints4s.algebra.server.{
  BasicAuthenticationTestSuite,
  ChunkedJsonEntitiesTestSuite,
  DecodedUrl,
  EndpointsTestSuite
}
import org.http4s.server.Router
import org.http4s.{HttpRoutes, Uri}
import org.http4s.syntax.kleisli._

import scala.concurrent.ExecutionContext
import akka.stream.scaladsl.Source

import scala.concurrent.Future
import akka.actor.ActorSystem

import ConverterSyntax._
import akka.stream.Materializer

import cats.effect.unsafe.implicits.global
import org.http4s.blaze.server.BlazeServerBuilder
import cats.effect.IO

class ChunkedEntitiesServerInterpreterTest
    extends EndpointsTestSuite[ChunkedEntitiesEndpointsTestApi]
    with BasicAuthenticationTestSuite[ChunkedEntitiesEndpointsTestApi]
    with ChunkedJsonEntitiesTestSuite[ChunkedEntitiesEndpointsTestApi] {

  val serverApi = new ChunkedEntitiesEndpointsTestApi()

  implicit val system = ActorSystem()
  implicit val materializer: Materializer = Materializer.createMaterializer(system)

  override def serveStreamedEndpoint[Req, Resp, Mat](
      endpoint: serverApi.Endpoint[Req, serverApi.Chunks[Resp]],
      response: Source[Resp, Mat]
  )(runTests: Int => Unit): Unit = {
    val port = {
      val socket = new ServerSocket(0)
      try socket.getLocalPort
      finally if (socket != null) socket.close()
    }

    // Akka Stream to fs2 stream conversion based on https://github.com/krasserm/streamz
    val stream =
      response
        .preMaterialize()
        ._2
        .toStream

    val service = HttpRoutes.of[IO](endpoint.implementedBy(_ => stream))
    val httpApp = Router("/" -> service).orNotFound
    val server = BlazeServerBuilder[IO](implicitly[ExecutionContext])
      .bindHttp(port, "localhost")
      .withHttpApp(httpApp)
    server.resource.use(_ => IO(runTests(port))).start.unsafeRunSync()
    ()
  }

  override def serveStreamedEndpoint[Req, Resp](
      endpoint: serverApi.Endpoint[serverApi.Chunks[Req], Resp],
      logic: Source[Req, _] => Future[Resp]
  )(runTests: Int => Unit): Unit = {
    val port = {
      val socket = new ServerSocket(0)
      try socket.getLocalPort
      finally if (socket != null) socket.close()
    }

    // Akka Stream to fs2 stream conversion based on https://github.com/krasserm/streamz
//    val stream = response.preMaterialize._2.toStream[IO]//(cs, implicitly[Async[IO]], implicitly[Materializer], ???)

    val service = HttpRoutes.of[IO](
      endpoint.implementedByEffect((reqStream: fs2.Stream[IO, Req]) => {
        IO.fromFuture(IO.delay(logic(akka.stream.scaladsl.Source.fromGraph(reqStream.toSource))))
      })
    )
    val httpApp = Router("/" -> service).orNotFound
    val server =
      BlazeServerBuilder[IO](implicitly[ExecutionContext])
        .bindHttp(port, "localhost")
        .withHttpApp(httpApp)
    server.resource.use(_ => IO(runTests(port))).start.unsafeRunSync()
    ()
  }

  override def serveEndpoint[Req, Resp](endpoint: serverApi.Endpoint[Req, Resp], response: => Resp)(
      runTests: Int => Unit
  ): Unit = {
    val port = {
      val socket = new ServerSocket(0)
      try socket.getLocalPort
      finally if (socket != null) socket.close()
    }
    val service = HttpRoutes.of[IO](endpoint.implementedBy(_ => response))
    val httpApp = Router("/" -> service).orNotFound
    val server =
      BlazeServerBuilder[IO](implicitly[ExecutionContext])
        .bindHttp(port, "localhost")
        .withHttpApp(httpApp)
    server.resource.use(_ => IO(runTests(port))).unsafeRunSync()
    ()
  }

  override def serveIdentityEndpoint[Resp](endpoint: serverApi.Endpoint[Resp, Resp])(
      runTests: Int => Unit
  ): Unit = {
    val port = {
      val socket = new ServerSocket(0)
      try socket.getLocalPort
      finally if (socket != null) socket.close()
    }
    val service = HttpRoutes.of[IO](endpoint.implementedBy(identity[Resp]))
    val httpApp = Router("/" -> service).orNotFound
    val server =
      BlazeServerBuilder[IO](implicitly[ExecutionContext])
        .bindHttp(port, "localhost")
        .withHttpApp(httpApp)
    server.resource.use(_ => IO(runTests(port))).unsafeRunSync()
    ()
  }

  def decodeUrl[A](url: serverApi.Url[A])(rawValue: String): DecodedUrl[A] = {
    val uri =
      Uri.fromString(rawValue).getOrElse(sys.error(s"Illegal URI: $rawValue"))

    url.decodeUrl(uri) match {
      case None                  => DecodedUrl.NotMatched
      case Some(Invalid(errors)) => DecodedUrl.Malformed(errors)
      case Some(Valid(a))        => DecodedUrl.Matched(a)
    }
  }

}
