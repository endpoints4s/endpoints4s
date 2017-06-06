package cqrs.publicserver

import endpoints.play.server.{Assets, Endpoints}
import play.api.routing.{Router => PlayRouter}
import play.twirl.api.{Html, StringInterpolation}

/**
  * These endpoints serve the web page and the assets.
  */
object BootstrapEndpoints extends Endpoints with Assets {

  val index: Endpoint[Unit, Html] =
    endpoint(get(path), htmlResponse)

  val assets: Endpoint[AssetRequest, AssetResponse] =
    assetsEndpoint(path / "assets" / assetSegments)

  val routes: PlayRouter.Routes =
    routesFromEndpoints(
      index.implementedBy(_ => indexHtml),
      assets.implementedBy(assetsResources(/*pathPrefix = Some("/public")*/))
    )

  lazy val digests = BootstrapDigests.digests

  lazy val indexHtml =
    html"""<!DOCTYPE html>
          <html>
            <head>
              <script src="${assets.call(asset("example-cqrs-web-client-fastopt.js")).url}" defer></script>
              <script src="${assets.call(asset("example-cqrs-web-client-launcher.js")).url}" defer></script>
              <title>Meters</title>
            </head>
            <body></body>
          </html>
        """

}
