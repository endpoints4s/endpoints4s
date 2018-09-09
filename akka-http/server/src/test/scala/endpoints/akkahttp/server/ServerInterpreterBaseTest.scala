package endpoints.akkahttp.server

import java.net.ServerSocket

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import endpoints.algebra.server.{Server, ServerTestBase}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class ServerInterpreterBaseTest extends ServerTestBase[EndpointsTestApi] {

  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  override val serverApi: EndpointsTestApi = new EndpointsTestApi

  override def serveEndpoint[Resp](endpoint: serverApi.Endpoint[_, Resp], response: Resp): Server = {

    val routes: Route = endpoint.implementedBy(_ => response)

    new Server {
      private lazy val freePort = findOpenPort
      private lazy val serverBinding: Future[Http.ServerBinding] = spinUpServer(routes, freePort)

      override val port: Int = freePort

      override def start(): Unit = {
        Await.result(serverBinding, 10.seconds)
        println(s"Akka-http test server started on port $freePort")
      }

      override def stop(): Unit = {
        Await.result(serverBinding.flatMap(_.unbind()), 10.seconds)
        println("Akka-http test server stopped successfully")
      }
    }

  }

  private def spinUpServer(rootRoute: Route, port: Int) = {
    Http().bindAndHandle(rootRoute, "localhost", port)
  }

  private def findOpenPort: Int = {
    val socket = new ServerSocket(0)
    try socket.getLocalPort
    finally if (socket != null) socket.close()
  }



}
