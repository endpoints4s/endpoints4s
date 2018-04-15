package endpoints.play.server

import endpoints.algebra

// not really a test, just verifies algebra compatibility
class EndpointsTestApi(val playComponents: PlayComponents, val digests: Map[String, String])
  extends Endpoints
  with BasicAuthentication
  with Assets
  with algebra.BasicAuthTestApi
  with algebra.EndpointsTestApi
  with algebra.Assets
//  with JsonFromCodecTestApi