package sample

import _root_.play.api.http.ContentTypes.HTML
import _root_.play.api.mvc.{Handler, RequestHeader, Results}
import _root_.play.api.routing.sird._
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
          |    <script src="/assets/sample-client-fastopt.js"></script>
          |    <script>sample.Main().main();</script>
          |  </body>
          |</html>
        """.stripMargin
      Ok(html).as(HTML)
    }
    case GET(p"/assets/sample-client-fastopt.js") =>
      assets.versioned("/", "sample-client-fastopt.js")
    case GET(p"/api/description") => action {
      import endpoints.play.server.circe.Util.circeJsonWriteable
      import io.circe.syntax._
      Ok(sample.openapi.DocumentedApi.documentation.asJson)
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

  val api = new Api(playComponents)
  val documentedApi = new DocumentedApi(playComponents)

  HttpServer(config, playComponents, bootstrap orElse api.routes orElse documentedApi.routes)

}