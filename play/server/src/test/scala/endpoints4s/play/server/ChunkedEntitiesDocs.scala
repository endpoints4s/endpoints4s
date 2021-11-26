package endpoints4s.play.server

import akka.stream.scaladsl.Source
import endpoints4s.algebra
import scala.concurrent.duration.DurationInt

trait ChunkedEntitiesDefinitions extends algebra.ChunkedEntities {
  //#streamed-endpoint
  val logo: Endpoint[Unit, Chunks[Array[Byte]]] =
    endpoint(get(path / "logo.png"), ok(bytesChunksResponse))
  //#streamed-endpoint
}

trait ChunkedEntitiesDocs extends ChunkedEntitiesDefinitions with ChunkedEntities {
  //#implementation
  import akka.stream.scaladsl.FileIO
  import java.nio.file.Paths

  val logoHandler =
    logo.implementedBy { _ =>
      FileIO.fromPath(Paths.get("/foo/bar/logo.png")).map(_.toArray)
    }
  //#implementation
}

trait JsonStreamingExample
  extends algebra.Endpoints
    with algebra.ChunkedJsonEntities
    with algebra.JsonEntitiesFromSchemas {
  val ticks = endpoint(get(path / "ticks"), ok(jsonChunksResponse[Unit]))
}

//#json-streaming
import endpoints4s.play.server

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
