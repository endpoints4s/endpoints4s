package sample

import _root_.play.api.mvc.{Action, Handler, RequestHeader, Results}
import _root_.play.api.http.ContentTypes.HTML
import _root_.play.core.server.NettyServer
import _root_.play.api.routing.sird._
import _root_.play.api.{Configuration, Environment}
import _root_.play.api.http.HttpConfiguration.HttpConfigurationProvider
import _root_.play.api.http.DefaultFileMimeTypesProvider
import sample.play.server.DocumentedApi

object Server extends App with Results {

  val bootstrap: PartialFunction[RequestHeader, Handler] = {
    case GET(p"/") => Action {
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
      controllers.Assets.versioned("/", "sample-client-fastopt.js")
    case GET(p"/api/description") => Action {
      import endpoints.play.server.Util.circeJsonWriteable
      import io.circe.syntax._
      Ok(sample.openapi.DocumentedApi.documentation.asJson)
    }
    case GET(p"/api/ui") => Action {
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

  val httpConfiguration = new HttpConfigurationProvider(Configuration.load(Environment.simple()), Environment.simple()).get
  val fileMimeTypes = new DefaultFileMimeTypesProvider(httpConfiguration.fileMimeTypes).get
  val api = new Api(fileMimeTypes)

  NettyServer.fromRouter()(bootstrap orElse api.routes orElse DocumentedApi.routes)

}