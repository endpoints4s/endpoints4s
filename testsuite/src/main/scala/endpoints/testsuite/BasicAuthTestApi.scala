package endpoints.testsuite

import endpoints.algebra
import endpoints.algebra.BasicAuthBuilders

trait BasicAuthTestApi extends algebra.Endpoints with algebra.BasicAuthentication with BasicAuthBuilders {


  val protectedEndpoint = authenticatedEndpoint(
    Get,
    path / "users",
    emptyRequest,
    textResponse
  )

  val protectedEndpointViaBuilder =
    anEndpoint
      .withMethod(Get)
      .withUrl(path / "users")
      .withTextResponse
      .withBasicAuth
      .build


}
