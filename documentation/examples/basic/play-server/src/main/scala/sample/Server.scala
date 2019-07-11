package sample

import _root_.play.api.http.ContentTypes.HTML
import _root_.play.api.mvc.{Handler, RequestHeader, Results}
import _root_.play.api.routing.sird._
import _root_.play.api.libs.json.Json
import _root_.play.core.server.ServerConfig
import controllers.{AssetsBuilder, AssetsConfiguration, DefaultAssetsMetadata}
import endpoints.play.server.{DefaultPlayComponents, HttpServer}
import sample.play.server.DocumentedApi

object Server extends App with Results {

  val config = ServerConfig()
  val playComponents = new DefaultPlayComponents(config)
  val assetsMetadata = new DefaultAssetsMetadata(playComponents.environment, AssetsConfiguration.fromConfiguration(playComponents.configuration), playComponents.fileMimeTypes)
  val assets = new AssetsBuilder(playComponents.httpErrorHandler, assetsMetadata)
  val action = playComponents.actionBuilder

  val bootstrap: PartialFunction[RequestHeader, Handler] = {
    case GET(p"/") => action {
      val html =
        """
          |<!DOCTYPE html>
          |<html>
          |  <head>
          |  </head>
          |  <body>
          |    <script src="/assets/app.js"></script>
          |  </body>
          |</html>
        """.stripMargin
      Ok(html).as(HTML)
    }
    case GET(p"/assets/app.js") =>
      assets.versioned("/", "example-basic-client-fastopt.js")
    case GET(p"/api/description") => action {
      import sample.openapi.OpenApiEncoder.JsonSchema._
      Ok(Json.toJson(sample.openapi.DocumentedApi.documentation))
    }
    case GET(p"/api/ui") => action {
      val html =
        """
          |<!DOCTYPE html>
          |<html>
          |  <head>
          |    <title>ReDoc</title>
          |    <!-- needed for adaptive design -->
          |    <meta charset="utf-8"/>
          |    <meta name="viewport" content="width=device-width, initial-scale=1">
          |
          |    <!--
          |    ReDoc doesn't change outer page styles
          |    -->
          |    <style>
          |      body {
          |        margin: 0;
          |        padding: 0;
          |      }
          |    </style>
          |  </head>
          |  <body>
          |    <redoc spec-url='/api/description'></redoc>
          |    <script src="https://rebilly.github.io/ReDoc/releases/latest/redoc.min.js"> </script>
          |  </body>
          |</html>
        """.stripMargin
      Ok(html).as(HTML)
    }
  }

  val jsonStreamingExample = new JsonStreamingExampleServer(playComponents)
  val api = new Api(playComponents)
  val documentedApi = new DocumentedApi(playComponents)

  HttpServer(config, playComponents, bootstrap orElse api.routes orElse jsonStreamingExample.routes orElse documentedApi.routes)

}