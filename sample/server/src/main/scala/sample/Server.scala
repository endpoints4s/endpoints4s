package sample

import play.api.mvc.{Action, Handler, RequestHeader, Results}
import play.api.http.ContentTypes.HTML
import play.core.server.NettyServer
import play.api.routing.sird._

object Server extends App with Results {

  NettyServer.fromRouter()(({
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
  }: PartialFunction[RequestHeader, Handler]) orElse Api.routes)

}