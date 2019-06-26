package endpoints.http4s.server

import cats.effect.{IO, Sync}
import endpoints.algebra

class EndpointsTestApi
    extends Endpoints
    with BasicAuthentication
    with algebra.BasicAuthTestApi
    with algebra.EndpointsTestApi {

  type Effect[A] = IO[A]

  override implicit def Effect: Sync[IO] = Sync[IO]
}
