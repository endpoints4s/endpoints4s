package sample

import _root_.play.api.mvc.{Action, Handler, RequestHeader, Results}
import _root_.play.api.http.ContentTypes.HTML
import _root_.play.core.server.NettyServer
import _root_.play.api.routing.sird._
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
      import endpoints.play.PlayCirce._
      import io.circe.syntax._
      Ok(sample.openapi.DocumentedApi.documentation.asJson)
    }
  }

  NettyServer.fromRouter()(bootstrap orElse Api.routes orElse DocumentedApi.delegate.routes)

}