package endpoints4s.akkahttp.server

import endpoints4s.algebra

import scala.annotation.nowarn

/* implements the endpoint using a codecs-based json handling */
@nowarn("cat=deprecation")
class EndpointsCodecsTestApi
    extends EndpointsTestApi
    with algebra.JsonFromCodecTestApi
    with algebra.circe.JsonFromCirceCodecTestApi
    with algebra.ChunkedJsonEntitiesTestApi
    with algebra.circe.ChunkedJsonEntitiesTestApi
    with algebra.AuthenticatedEndpointsTestApi
    with algebra.AuthenticatedEndpointsServer
    with algebra.BasicAuthenticationTestApi
    with algebra.SumTypedEntitiesTestApi
    with JsonEntitiesFromCodecs
    with ChunkedJsonEntities
    with BasicAuthentication
