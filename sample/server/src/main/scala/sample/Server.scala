package sample

import controllers.Assets
import play.api.mvc.{Results, Action}
import play.api.http.ContentTypes.HTML
import play.core.server.NettyServer
import play.api.routing.sird._

object Server extends App with Results {

  NettyServer.fromRouter()(Api.routes orElse {
    case GET(p"/") => Action {
      val html =
        """
          |<!DOCTYPE html>
          |<html>
          |  <head>
          |  </head>
          |  <body>
          |    <script src="/assets/sample-client-fastopt.js"></script>
          |    <script>example.Main().main();</script>
          |  </body>
          |</html>
        """.stripMargin
      Ok(html).as(HTML)
    }
    case GET(p"/assets/$file*") =>
      Assets.versioned("/", file)
  })

}