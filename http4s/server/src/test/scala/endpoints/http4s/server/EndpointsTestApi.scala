package endpoints.http4s.server

import cats.effect.IO
import endpoints.algebra

class EndpointsTestApi
    extends Endpoints[IO]
    with BasicAuthentication
    with JsonEntitiesFromSchemas
    with algebra.EndpointsTestApi
    with algebra.BasicAuthenticationTestApi
    with algebra.JsonEntitiesFromSchemasTestApi
    with algebra.TextEntitiesTestApi
