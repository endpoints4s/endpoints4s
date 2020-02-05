package endpoints.akkahttp.server

import endpoints.algebra

/* implements the endpoint using a codecs-based json handling */
class EndpointsCodecsTestApi extends EndpointsTestApi
  with algebra.JsonFromCodecTestApi
  with algebra.circe.JsonFromCirceCodecTestApi
  with algebra.ChunkedJsonEntitiesTestApi
  with algebra.circe.ChunkedJsonEntitiesTestApi
  with algebra.BasicAuthenticationTestApi
  with JsonEntitiesFromCodecs
  with ChunkedJsonEntities
  with BasicAuthentication
