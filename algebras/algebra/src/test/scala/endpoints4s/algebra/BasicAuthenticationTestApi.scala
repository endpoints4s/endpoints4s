package endpoints4s.algebra

import endpoints4s.algebra

import scala.annotation.nowarn

@nowarn("cat=deprecation")
trait BasicAuthenticationTestApi extends EndpointsTestApi with algebra.BasicAuthentication {

  val protectedEndpoint = authenticatedEndpoint(
    Get,
    path / "users",
    ok(textResponse)
  )

  val protectedEndpointWithParameter = authenticatedEndpoint(
    Get,
    path / "users" / segment[Long]("id"),
    ok(textResponse)
  )

}
