package endpoints.http4s.server

import java.net.ServerSocket

import cats.effect.{ContextShift, IO, Timer}
import endpoints.{Invalid, Valid}
import endpoints.algebra.server.{
  BasicAuthenticationTestSuite,
  DecodedUrl,
  EndpointsTestSuite
}
import org.http4s.server.Router
import org.http4s.{HttpRoutes, Uri}
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.syntax.kleisli._

import scala.concurrent.ExecutionContext
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.Source
import scala.concurrent.Future
import akka.actor.ActorSystem
import fs2.concurrent.Queue
import endpoints.algebra.server.JsonEntitiesFromSchemasTestSuite

class JsonSchemaInterpreterTest
    extends EndpointsTestSuite[JsonSchemaEndpointsTestApi]
    with BasicAuthenticationTestSuite[JsonSchemaEndpointsTestApi]
    with JsonEntitiesFromSchemasTestSuite[JsonSchemaEndpointsTestApi] {

  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)
  implicit val system = ActorSystem()

  def serveStreamedEndpoint[Resp](endpoint: serverApi.Endpoint[_, fs2.Stream[IO, Resp]], response: Source[Resp, _])(runTests: Int => Unit): Unit = {
    val port = {
      val socket = new ServerSocket(0)
      try socket.getLocalPort
      finally if (socket != null) socket.close()
    }
    
    val stream = for {
      q <- fs2.Stream.eval(Queue.unbounded[IO, Option[Resp]])
      _ <- fs2.Stream.eval(
        IO.fromFuture(IO(response.runForeach(r => q.enqueue1(Some(r)))))
      )
      _ <- fs2.Stream.eval(q.enqueue1(None))
      res <- q.dequeue.unNone
    } yield res

    val service = HttpRoutes.of[IO](endpoint.implementedBy(_ => stream))
    val httpApp = Router("/" -> service).orNotFound
    val server =
      BlazeServerBuilder[IO].bindHttp(port, "localhost").withHttpApp(httpApp)
    val fiber = server.resource.use(_ => IO.never).start.unsafeRunSync()
    try {
      runTests(port)
    } finally {
      fiber.cancel.unsafeRunSync()
    }
  }
    

  def serveStreamedEndpoint[Req, Resp](endpoint: serverApi.Endpoint[fs2.Stream[IO, Req],Resp], logic: Source[Req, _] => Future[Resp])(runTests: Int => Unit): Unit = {
    val port = {
      val socket = new ServerSocket(0)
      try socket.getLocalPort
      finally if (socket != null) socket.close()
    }

    def toSource(s: fs2.Stream[IO, Req]): Source[Req, _]=
      Source.fromIterator(() => s.compile.to(Iterator).unsafeRunSync())
    

    val service = HttpRoutes.of[IO](endpoint.implementedByEffect(stream => IO.fromFuture(IO(logic(toSource(stream))))))
    val httpApp = Router("/" -> service).orNotFound
    val server =
      BlazeServerBuilder[IO].bindHttp(port, "localhost").withHttpApp(httpApp)
    val fiber = server.resource.use(_ => IO.never).start.unsafeRunSync()
    try {
      runTests(port)
    } finally {
      fiber.cancel.unsafeRunSync()
    }
  }


  val serverApi = new JsonSchemaEndpointsTestApi()

  def decodeUrl[A](url: serverApi.Url[A])(rawValue: String): DecodedUrl[A] = {
    val uri =
      Uri.fromString(rawValue).getOrElse(sys.error(s"Illegal URI: $rawValue"))

    url.decodeUrl(uri) match {
      case None                  => DecodedUrl.NotMatched
      case Some(Invalid(errors)) => DecodedUrl.Malformed(errors)
      case Some(Valid(a))        => DecodedUrl.Matched(a)
    }
  }

  def serveEndpoint[Resp](
      endpoint: serverApi.Endpoint[_, Resp],
      response: => Resp
  )(runTests: Int => Unit): Unit = {
    val port = {
      val socket = new ServerSocket(0)
      try socket.getLocalPort
      finally if (socket != null) socket.close()
    }
    val service = HttpRoutes.of[IO](endpoint.implementedBy(_ => response))
    val httpApp = Router("/" -> service).orNotFound
    val server =
      BlazeServerBuilder[IO].bindHttp(port, "localhost").withHttpApp(httpApp)
    val fiber = server.resource.use(_ => IO.never).start.unsafeRunSync()
    try {
      runTests(port)
    } finally {
      fiber.cancel.unsafeRunSync()
    }
  }

}
