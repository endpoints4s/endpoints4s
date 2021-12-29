package endpoints4s.http4s.server

import cats.effect.IO
import endpoints4s.algebra

class ChunkedEntitiesEndpointsTestApi
    extends Endpoints[IO]
    with BasicAuthentication
    with JsonEntitiesFromCodecs
    with ChunkedJsonEntities
    with algebra.EndpointsTestApi
    with algebra.BasicAuthenticationTestApi
    with algebra.circe.JsonFromCirceCodecTestApi
    with algebra.ChunkedEntitiesTestApi
    with algebra.ChunkedJsonEntitiesTestApi
    with algebra.circe.CounterCodecCirce
