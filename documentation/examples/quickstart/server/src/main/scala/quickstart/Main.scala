package quickstart

//#relevant-code
import endpoints.openapi.model.OpenApi
import endpoints.play.server
//#main-only
import endpoints.play.server.{DefaultPlayComponents, HttpServer, PlayComponents}
import play.core.server.ServerConfig

object Main extends App {
  val config = ServerConfig()
  val playComponents = new DefaultPlayComponents(config)
  val counterServer = new CounterServer(playComponents)
  val documentationServer = new DocumentationServer(playComponents)
  HttpServer(config, playComponents, counterServer.routes orElse documentationServer.routes)
}
//#main-only

// Additional route for serving the OpenAPI documentation
class DocumentationServer(val playComponents: PlayComponents)
  extends server.Endpoints
    with server.circe.JsonEntities {

  val routes = routesFromEndpoints(
    endpoint(get(path / "documentation.json"), jsonResponse[OpenApi]())
      .implementedBy(_ => CounterDocumentation.api)
  )

}
//#relevant-code