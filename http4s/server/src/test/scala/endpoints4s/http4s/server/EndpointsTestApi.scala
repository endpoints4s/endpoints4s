package endpoints4s.http4s.server

import cats.effect.IO
import endpoints4s.algebra

class EndpointsTestApi
    extends Endpoints[IO]
    with BasicAuthentication
    with JsonEntitiesFromSchemas
    with algebra.EndpointsTestApi
    with algebra.BasicAuthenticationTestApi
    with algebra.JsonEntitiesFromSchemasTestApi
    with algebra.TextEntitiesTestApi
    with algebra.SumTypedEntitiesTestApi {

  implicit def userCodec = userJsonSchema
}
