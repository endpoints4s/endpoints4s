package endpoints4s.fetch.future

import endpoints4s.algebra
import endpoints4s.algebra.EndpointsTestApi

trait BasicAuthenticationTestApi extends EndpointsTestApi with algebra.BasicAuthentication {

  val successProtectedEndpoint = authenticatedEndpoint(
    Get,
    path / "basic-auth" / "success",
    ok(textResponse)
  )

  val failureProtectedEndpoint = authenticatedEndpoint(
    Get,
    path / "basic-auth" / "failure",
    ok(textResponse)
  )
}
