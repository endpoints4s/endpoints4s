package endpoints.akkahttp.server

import java.net.ServerSocket

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import endpoints.algebra.server.ServerTestBase

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class ServerInterpreterBaseTest extends ServerTestBase[EndpointsTestApi] {

  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val serverApi: EndpointsTestApi = new EndpointsTestApi

  def serveEndpoint[Resp](endpoint: serverApi.Endpoint[_, Resp], response: Resp)(runTests: Int => Unit): Unit = {

    val routes: Route = endpoint.implementedBy(_ => response)
    val port = {
      val socket = new ServerSocket(0)
      try socket.getLocalPort
      finally if (socket != null) socket.close()
    }
    val serverBinding: Future[Http.ServerBinding] =
      Http().bindAndHandle(routes, "localhost", port)

    Await.result(serverBinding, 10.seconds)
    runTests(port)
    Await.result(serverBinding.flatMap(_.unbind()), 10.seconds)
    ()
  }

}
