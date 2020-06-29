package endpoints4s.akkahttp.server

import endpoints4s.algebra

/* implements the endpoint using a codecs-based json handling */
class EndpointsCodecsTestApi
    extends EndpointsTestApi
    with algebra.JsonFromCodecTestApi
    with algebra.circe.JsonFromCirceCodecTestApi
    with algebra.ChunkedJsonEntitiesTestApi
    with algebra.circe.ChunkedJsonEntitiesTestApi
    with algebra.BasicAuthenticationTestApi
    with algebra.SumTypedEntitiesTestApi
    with JsonEntitiesFromCodecs
    with ChunkedJsonEntities
    with BasicAuthentication
