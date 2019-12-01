package endpoints.akkahttp.server

import endpoints.algebra

/* implements the endpoint using a codecs-based json handling */
class EndpointsCodecsTestApi extends EndpointsTestApi
  with algebra.JsonFromCodecTestApi
  with algebra.circe.JsonFromCirceCodecTestApi
  with JsonEntitiesFromCodecs
