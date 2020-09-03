package quickstart

//#relevant-code
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._

object Main extends App {
  implicit val system: ActorSystem = ActorSystem("server-system")
  val routes = CounterServer.routes ~ DocumentationServer.routes
  Http().newServerAt("0.0.0.0", 8000).bindFlow(routes)
}

// Additional route for serving the OpenAPI documentation
//#serving-documentation
import endpoints4s.openapi.model.OpenApi
import endpoints4s.akkahttp.server

object DocumentationServer
    extends server.Endpoints
    with server.JsonEntitiesFromEncodersAndDecoders {

  val routes =
    endpoint(get(path / "documentation.json"), ok(jsonResponse[OpenApi]))
      .implementedBy(_ => CounterDocumentation.api)

}
//#serving-documentation
//#relevant-code
