package endpoints4s.play.server

import endpoints4s.algebra
import endpoints4s.algebra.circe.JsonFromCirceCodecTestApi

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
    with algebra.SumTypedEntitiesTestApi
    with algebra.TextEntitiesTestApi
    with algebra.Assets
    with JsonFromCirceCodecTestApi
    with algebra.ChunkedEntitiesTestApi
    with algebra.ChunkedJsonEntitiesTestApi
    with algebra.circe.CounterCodecCirce
