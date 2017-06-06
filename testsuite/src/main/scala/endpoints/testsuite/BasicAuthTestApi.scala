package endpoints.testsuite

import endpoints.algebra

/**
  * Created by wpitula on 6/5/17.
  */
trait BasicAuthTestApi extends algebra.Endpoints with algebra.BasicAuthentication {


  val protectedEndpoint = authenticatedEndpoint(
    Get,
    path / "users",
    emptyRequest,
    stringResponse
  )

}
