package endpoints.algebra

trait BasicAuthTestApi {

  val basicAuth: BasicAuthentication
  import basicAuth.endpoints.requests.methods._
  import basicAuth.endpoints.requests.urls._
  import basicAuth.endpoints.responses._


  val protectedEndpoint = basicAuth.authenticatedEndpoint(
    Get,
    path / "users",
    textResponse()
  )

}
