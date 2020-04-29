package endpoints.http4s.server

import cats.effect.IO
import endpoints.algebra

class JsonSchemaEndpointsTestApi
    extends Endpoints[IO]
    with BasicAuthentication
    with JsonEntitiesFromSchemas
    with ChunkedJsonEntities
    with algebra.EndpointsTestApi
    with algebra.BasicAuthenticationTestApi
    with algebra.JsonEntitiesFromSchemasTestApi
    with algebra.JsonEntitiesFromSchemas
  