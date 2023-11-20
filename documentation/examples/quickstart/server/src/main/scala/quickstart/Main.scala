package quickstart

//#relevant-code
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.server.Directives._

object Main extends App {
  implicit val system: ActorSystem = ActorSystem("server-system")
  val routes = CounterServer.routes ~ DocumentationServer.routes
  Http().newServerAt("0.0.0.0", 8000).bindFlow(routes)
}

// Additional route for serving the OpenAPI documentation
//#serving-documentation
import endpoints4s.openapi.model.OpenApi
import endpoints4s.pekkohttp.server

object DocumentationServer
    extends server.Endpoints
    with server.JsonEntitiesFromEncodersAndDecoders {

  val routes =
    endpoint(get(path / "documentation.json"), ok(jsonResponse[OpenApi]))
      .implementedBy(_ => CounterDocumentation.api)

}
//#serving-documentation
//#relevant-code
