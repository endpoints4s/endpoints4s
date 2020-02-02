package sample

import _root_.play.api.http.ContentTypes.{HTML, JSON}
import _root_.play.api.mvc.{Handler, RequestHeader, Results}
import _root_.play.api.routing.sird._
import _root_.play.api.routing.Router
import _root_.play.core.server.DefaultNettyServerComponents
import controllers.{AssetsBuilder, AssetsConfiguration, DefaultAssetsMetadata}
import endpoints.openapi.model.OpenApi
import endpoints.play.server.PlayComponents
import sample.play.server.DocumentedApi

object Server extends App with Results with DefaultNettyServerComponents {

  lazy val playComponents = PlayComponents.fromBuiltInComponents(this)
  lazy val assetsMetadata = new DefaultAssetsMetadata(environment, AssetsConfiguration.fromConfiguration(configuration), fileMimeTypes)
  lazy val assets = new AssetsBuilder(httpErrorHandler, assetsMetadata)
  lazy val action = defaultActionBuilder

  lazy val bootstrap: PartialFunction[RequestHeader, Handler] = {
    case GET(p"/") => action {
      val html =
        """
          |<!DOCTYPE html>
          |<html>
          |  <head>
          |  </head>
          |  <body>
          |    <script src="/assets/app.js"></script>
          |    <script>sample.Main().main();</script>
          |  </body>
          |</html>
        """.stripMargin
      Ok(html).as(HTML)
    }
    case GET(p"/assets/app.js") =>
      assets.versioned("/", "example-basic-client-fastopt.js")
    case GET(p"/api/description") => action {
      Ok(OpenApi.stringEncoder.encode(sample.openapi.DocumentedApi.documentation)).as(JSON)
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

  lazy val api = new Api(playComponents)
  lazy val documentedApi = new DocumentedApi(playComponents)

  lazy val router = Router.from(bootstrap orElse api.routes orElse documentedApi.routes)

  server
}