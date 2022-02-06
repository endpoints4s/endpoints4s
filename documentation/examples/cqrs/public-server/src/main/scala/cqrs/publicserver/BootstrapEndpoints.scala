package cqrs.publicserver

import cats.effect.{IO, Sync}
import endpoints4s.openapi.model.OpenApi
import endpoints4s.http4s.server
import org.http4s.{EntityEncoder, MediaType}
import org.http4s.headers.`Content-Type`

/** These endpoints serve the web page and the assets.
  */
class BootstrapEndpoints
    extends server.Endpoints[IO]
    with server.Assets
    with server.JsonEntitiesFromEncodersAndDecoders {

  val index: Endpoint[Unit, String] =
    endpoint(get(path), ok(htmlResponse))

  val assets: Endpoint[AssetRequest, AssetResponse] =
    assetsEndpoint(path / "assets" / assetSegments())

  val documentation: Endpoint[Unit, OpenApi] = {
    endpoint(get(path / "documentation"), ok(jsonResponse[OpenApi]))
  }

  val routes =
    routesFromEndpoints(
      index.implementedBy(_ => indexHtml),
      assets.implementedBy(assetsResources( /*pathPrefix = Some("/public")*/ ))
    )

  lazy val indexHtml =
    """<!DOCTYPE html>
          <html>
            <head>
              <script src="/assets/example-cqrs-web-client-fastopt.js" defer></script>
              <title>Meters</title>
            </head>
            <body></body>
          </html>
        """

  lazy val htmlResponse: ResponseEntity[String] =
    EntityEncoder[Effect, String]
      .withContentType(`Content-Type`(MediaType.text.html))

  def EffectSync: Sync[Effect] = Sync[IO]

}
