package sample

import scala.concurrent.duration.DurationInt

//#json-streaming
import akka.stream.scaladsl.Source
import endpoints.akkahttp.server
import endpoints.akkahttp.server.Http1Streaming.Settings

class JsonStreamingExampleServer(val settings: Settings)
  extends JsonStreamingExample
    with server.Http1JsonStreaming
    with server.JsonSchemaEntities {

  val routes =
    ticks.implementedBy(_ => Source.tick(0.seconds, 1.second, ()))

}
//#json-streaming
