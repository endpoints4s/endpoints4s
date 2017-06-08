package endpoints.testsuite

import endpoints.algebra

trait BasicAuthTestApi extends algebra.Endpoints with algebra.BasicAuthentication {


  val protectedEndpoint = authenticatedEndpoint(
    Get,
    path / "users",
    emptyRequest,
    textResponse
  )

}
