package quickstart

//#relevant-code
import endpoints.openapi.model.{OpenApi, OpenApiSchemas}
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
    with OpenApiSchemas with server.playjson.JsonSchemaEntities {

  val routes = routesFromEndpoints(
    endpoint[Unit, OpenApi](get(path / "documentation.json"), ok(jsonResponse[OpenApi]))
      .implementedBy(_ => CounterDocumentation.api)
  )

}
//#relevant-code