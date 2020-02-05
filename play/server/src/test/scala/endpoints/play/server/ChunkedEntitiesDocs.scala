package endpoints.play.server


import akka.stream.scaladsl.Source
import endpoints.algebra
import endpoints.algebra.JsonStreamingExample
import scala.concurrent.duration.DurationInt

trait ChunkedEntitiesDocs extends algebra.ChunkedEntitiesDocs with ChunkedEntities  {

  //#implementation
  import akka.stream.scaladsl.FileIO
  import java.nio.file.Paths

  val logoHandler =
    logo.implementedBy { _ =>
      FileIO.fromPath(Paths.get("/foo/bar/logo.png")).map(_.toArray)
    }
  //#implementation

}

//#json-streaming
import endpoints.play.server

class JsonStreamingExampleServer(val playComponents: server.PlayComponents)
  extends JsonStreamingExample
    with server.Endpoints
    with server.ChunkedJsonEntities
    with server.JsonEntitiesFromSchemas {

  val routes = routesFromEndpoints(
    ticks.implementedBy(_ => Source.tick(0.seconds, 1.second, ()))
  )

}
//#json-streaming
