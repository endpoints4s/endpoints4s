package quickstart

//#relevant-code
import endpoints.openapi.model.{OpenApi, OpenApiSchemas}
import endpoints.play.server
import play.api.BuiltInComponents
import play.core.server.NettyServer
//#main-only
import play.core.server.ServerConfig

object Main extends App {
  val config = ServerConfig()
  NettyServer.fromRouterWithComponents(ServerConfig()) { playComponents =>
    val counterServer = new CounterServer(playComponents)
    val documentationServer = new DocumentationServer(playComponents)
    counterServer.routes orElse documentationServer.routes
  }
}
//#main-only

// Additional route for serving the OpenAPI documentation
class DocumentationServer(val playComponents: BuiltInComponents)
  extends server.Endpoints
    with OpenApiSchemas with server.playjson.JsonSchemaEntities {

  val routes = routesFromEndpoints(
    endpoint(get(path / "documentation.json"), jsonResponse[OpenApi]())
      .implementedBy(_ => CounterDocumentation.api)
  )

}
//#relevant-code