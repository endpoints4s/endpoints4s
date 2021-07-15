package endpoints4s.http4s.server

import cats.effect.IO
import endpoints4s.algebra

import scala.annotation.nowarn

@nowarn("cat=deprecation")
class EndpointsTestApi
    extends Endpoints[IO]
    with BasicAuthentication
    with JsonEntitiesFromSchemas
    with Assets
    with algebra.AssetsTestApi
    with algebra.EndpointsTestApi
    with algebra.AuthenticatedEndpointsTestApi
    with algebra.AuthenticatedEndpointsServer
    with algebra.BasicAuthenticationTestApi
    with algebra.JsonEntitiesFromSchemasTestApi
    with algebra.TextEntitiesTestApi
    with algebra.SumTypedEntitiesTestApi {

  implicit def userCodec = userJsonSchema

  type AssetContent = fs2.Stream[Effect, Byte]

  def noopAssetContent: fs2.Stream[Effect, Byte] = fs2.Stream.empty

  def notFoundAssetResponse: AssetResponse = AssetResponse.NotFound
}
