package endpoints.play.server

import endpoints.algebra
import endpoints.algebra.circe.JsonFromCirceCodecTestApi

// not really a test, just verifies algebra compatibility
class EndpointsTestApi(
    val playComponents: PlayComponents,
    val digests: Map[String, String]
) extends Endpoints
    with JsonEntitiesFromCodecs
    with BasicAuthentication
    with Assets
    with ChunkedJsonEntities
    with algebra.BasicAuthenticationTestApi
    with algebra.EndpointsTestApi
    with algebra.JsonFromCodecTestApi
    with algebra.TextEntitiesTestApi
    with algebra.Assets
    with JsonFromCirceCodecTestApi
    with algebra.circe.ChunkedJsonEntitiesTestApi
