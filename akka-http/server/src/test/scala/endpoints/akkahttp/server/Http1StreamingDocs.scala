package endpoints.akkahttp.server

import akka.http.scaladsl.server.Route
import endpoints.algebra

import scala.concurrent.Future

trait Http1StreamingDocs extends algebra.Http1StreamingDocs with Http1Streaming {

  //#implementation
  import java.nio.file.Paths
  import akka.stream.scaladsl.FileIO

  val logoRoute: Route =
    logo.implementedBy { _ =>
      FileIO.fromPath(Paths.get("/foo/bar/logo.png")).map(_.toArray)
    }
  //#implementation

  //#websocket-implementation
  import akka.stream.scaladsl.Flow

  val chatRoute: Route =
    chat.implementedBy { _ =>
      val serverFlow = Flow.fromFunction { message: String =>
        println(s"Received: $message")
        val response = "I agree"
        println(s"Sending: $response")
        response
      }
      Future.successful(Some(serverFlow))
    }
  //#websocket-implementation

}
