package endpoints4s.pekkohttp.server

import org.apache.pekko.http.scaladsl.server.Route
import endpoints4s.algebra

trait ChunkedEntitiesDefinitions extends algebra.ChunkedEntities {
  //#streamed-endpoint
  val logo: Endpoint[Unit, Chunks[Array[Byte]]] =
    endpoint(get(path / "logo.png"), ok(bytesChunksResponse))
  //#streamed-endpoint
}

trait ChunkedEntitiesDocs extends ChunkedEntitiesDefinitions with ChunkedEntities {
  //#implementation
  import java.nio.file.Paths
  import org.apache.pekko.stream.scaladsl.FileIO

  val logoRoute: Route =
    logo.implementedBy { _ =>
      FileIO.fromPath(Paths.get("/foo/bar/logo.png")).map(_.toArray)
    }
  //#implementation
}

import scala.concurrent.duration.DurationInt

trait JsonStreamingExample
    extends algebra.Endpoints
    with algebra.ChunkedJsonEntities
    with algebra.JsonEntitiesFromSchemas {

  val ticks =
    endpoint(get(path / "ticks"), ok(jsonChunksResponse[Unit](newLineDelimiterFraming)))

}

//#json-streaming
import org.apache.pekko.stream.scaladsl.Source
import endpoints4s.pekkohttp.server

object JsonStreamingExampleServer
    extends JsonStreamingExample
    with server.Endpoints
    with server.ChunkedJsonEntities
    with server.JsonEntitiesFromSchemas {

  val routes =
    ticks.implementedBy(_ => Source.tick(0.seconds, 1.second, ()))

}
//#json-streaming
