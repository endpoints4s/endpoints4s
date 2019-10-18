package endpoints.algebra

import endpoints.algebra

trait BasicAuthTestApi extends algebra.Endpoints with algebra.BasicAuthentication {


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
