package quickstart

//#relevant-code
import endpoints.openapi.model.OpenApi
import endpoints.play.server
import endpoints.play.server.PlayComponents
import play.core.server.NettyServer
//#main-only
import play.core.server.ServerConfig

object Main extends App {
  val config = ServerConfig()
  NettyServer.fromRouterWithComponents(config) { components =>
    val playComponents = PlayComponents.fromBuiltInComponents(components)
    val counterServer = new CounterServer(playComponents)
    val documentationServer = new DocumentationServer(playComponents)
    counterServer.routes orElse documentationServer.routes
  }
}
//#main-only

// Additional route for serving the OpenAPI documentation
//#serving-documentation
class DocumentationServer(val playComponents: PlayComponents)
  extends server.Endpoints with server.JsonEntitiesFromEncoderAndDecoder {

  val routes = routesFromEndpoints(
    endpoint[Unit, OpenApi](get(path / "documentation.json"), ok(jsonResponse[OpenApi]))
      .implementedBy(_ => CounterDocumentation.api)
  )

}
//#serving-documentation
//#relevant-code