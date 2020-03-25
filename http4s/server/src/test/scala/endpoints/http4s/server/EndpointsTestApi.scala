package endpoints.http4s.server

import cats.effect.IO
import endpoints.algebra

class EndpointsTestApi
    extends Endpoints[IO]
    with BasicAuthentication
    with JsonEntitiesFromCodecs
    with ChunkedJsonEntities
    with algebra.EndpointsTestApi
    with algebra.BasicAuthenticationTestApi
    with algebra.circe.JsonFromCirceCodecTestApi
    with algebra.ChunkedJsonEntitiesTestApi
    with algebra.circe.ChunkedJsonEntitiesTestApi
