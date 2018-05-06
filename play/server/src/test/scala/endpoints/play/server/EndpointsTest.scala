package endpoints.play.server

import endpoints.algebra
import endpoints.algebra.circe.JsonFromCirceCodecTestApi

// not really a test, just verifies algebra compatibility
class EndpointsTestApi(val playComponents: PlayComponents, val digests: Map[String, String])
  extends Endpoints
    with JsonEntitiesFromCodec
    with BasicAuthentication
    with Assets
    with algebra.BasicAuthTestApi
    with algebra.EndpointsTestApi
    with algebra.JsonFromCodecTestApi
    with algebra.Assets
    with JsonFromCirceCodecTestApi