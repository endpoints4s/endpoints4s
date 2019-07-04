package endpoints.akkahttp.server.playjson

import endpoints.akkahttp.server.JsonEntitiesFromCodec
import endpoints.algebra.{JsonFromCodecTestApi, playjson}
import endpoints.akkahttp.server.EndpointsTestApi 

/* implements the endpoint using a codecs-based json handling */
class EndpointsCodecsTestApi extends EndpointsTestApi
  with JsonFromCodecTestApi
  with playjson.JsonFromPlayJsonCodecTestApi
  with JsonEntitiesFromCodec
  with playjson.JsonEntitiesFromCodec
