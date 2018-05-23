package cqrs.publicserver

import endpoints.openapi.model.OpenApi
import endpoints.play.server.circe.JsonEntities
import endpoints.play.server.{Assets, Endpoints, PlayComponents}
import play.api.routing.{Router => PlayRouter}
import play.twirl.api.{Html, StringInterpolation}

/**
  * These endpoints serve the web page and the assets.
  */
class BootstrapEndpoints(protected val playComponents: PlayComponents) extends Endpoints with Assets with JsonEntities {

  val index: Endpoint[Unit, Html] =
    endpoint(get(path), htmlResponse)

  val assets: Endpoint[AssetRequest, AssetResponse] =
    assetsEndpoint(path / "assets" / assetSegments())

  val documentation: Endpoint[Unit, OpenApi] =
    endpoint(get(path / "documentation"), jsonResponse[OpenApi]())

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
              <title>Meters</title>
            </head>
            <body></body>
          </html>
        """

}
