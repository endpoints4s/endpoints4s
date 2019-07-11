package sample

import akka.stream.scaladsl.Source

import scala.concurrent.duration.DurationInt

//#json-streaming
import endpoints.play.server
import endpoints.play.server.PlayComponents

class JsonStreamingExampleServer(val playComponents: PlayComponents)
  extends JsonStreamingExample
    with server.Http1JsonStreaming
    with server.playjson.JsonSchemaEntities {

  val routes = routesFromEndpoints(
    ticks.implementedBy(_ => Source.tick(0.seconds, 1.second, ()))
  )

}
//#json-streaming
