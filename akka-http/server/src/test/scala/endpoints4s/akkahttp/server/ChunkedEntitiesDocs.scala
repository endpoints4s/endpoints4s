package endpoints4s.akkahttp.server

import akka.http.scaladsl.server.Route
import endpoints4s.algebra
import endpoints4s.algebra.JsonStreamingExample

trait ChunkedEntitiesDocs extends algebra.ChunkedEntitiesDocs with ChunkedEntities {

  //#implementation
  import java.nio.file.Paths
  import akka.stream.scaladsl.FileIO

  val logoRoute: Route =
    logo.implementedBy { _ =>
      FileIO.fromPath(Paths.get("/foo/bar/logo.png")).map(_.toArray)
    }
  //#implementation

}

import scala.concurrent.duration.DurationInt

//#json-streaming
import akka.stream.scaladsl.Source
import endpoints4s.akkahttp.server

object JsonStreamingExampleServer
    extends JsonStreamingExample
    with server.Endpoints
    with server.ChunkedJsonEntities
    with server.JsonEntitiesFromSchemas {

  val routes =
    ticks.implementedBy(_ => Source.tick(0.seconds, 1.second, ()))

}
//#json-streaming
