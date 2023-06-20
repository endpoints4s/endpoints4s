package endpoints4s.http4s.server

import java.net.ServerSocket

import endpoints4s.algebra.server.{
  ChunkedJsonEntitiesTestSuite,
  EndpointsTestSuite
}
import org.http4s.server.Router
import org.http4s.HttpRoutes

import akka.stream.scaladsl.Source

import scala.concurrent.Future
import akka.actor.ActorSystem

import ConverterSyntax._
import akka.stream.Materializer

import cats.effect.unsafe.implicits.global
import org.http4s.blaze.server.BlazeServerBuilder
import cats.effect.IO

class ChunkedEntitiesServerInterpreterTest
    extends Http4sServerTest[ChunkedEntitiesEndpointsTestApi]
    with EndpointsTestSuite[ChunkedEntitiesEndpointsTestApi]
    with ChunkedJsonEntitiesTestSuite[ChunkedEntitiesEndpointsTestApi] {

  val serverApi = new ChunkedEntitiesEndpointsTestApi()

  implicit val system: ActorSystem = ActorSystem()
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
    val server = BlazeServerBuilder[IO]
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
      BlazeServerBuilder[IO]
        .bindHttp(port, "localhost")
        .withHttpApp(httpApp)
    server.resource.use(_ => IO(runTests(port))).start.unsafeRunSync()
    ()
  }

}
