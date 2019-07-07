package endpoints.akkahttp.server.circe

import endpoints.akkahttp.server.JsonEntitiesFromCodec
import endpoints.algebra.{JsonFromCodecTestApi, circe}
import endpoints.akkahttp.server.EndpointsTestApi 

/* implements the endpoint using a codecs-based json handling */
class EndpointsCodecsTestApi extends EndpointsTestApi
  with JsonFromCodecTestApi
  with circe.JsonFromCirceCodecTestApi
  with JsonEntitiesFromCodec
  with circe.JsonEntitiesFromCodec

