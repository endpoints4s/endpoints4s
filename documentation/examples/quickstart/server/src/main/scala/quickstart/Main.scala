package quickstart

//#relevant-code
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object Main extends App {
  implicit val system: ActorSystem = ActorSystem("server-system")
  val routes = CounterServer.routes ~ DocumentationServer.routes
  val binding = Http().bindAndHandle(routes, "0.0.0.0", 8000)
  sys.addShutdownHook {
    Await.result(Await.result(binding, 10.seconds).terminate(3.seconds), 15.seconds)
    Await.result(system.terminate(), 5.seconds)
  }
}

// Additional route for serving the OpenAPI documentation
//#serving-documentation
import endpoints.openapi.model.OpenApi
import endpoints.akkahttp.server

object DocumentationServer
  extends server.Endpoints with server.JsonEntitiesFromEncodersAndDecoders {

  val routes =
    endpoint(get(path / "documentation.json"), ok(jsonResponse[OpenApi]))
      .implementedBy(_ => CounterDocumentation.api)

}
//#serving-documentation
//#relevant-code
