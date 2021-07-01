package endpoints4s.algebra

trait AuthenticatedEndpointsTestApi extends EndpointsTestApi with AuthenticatedEndpoints {

  val basicAuthEndpoint = endpoint(
    get(path / "users"),
    ok(textResponse)
  ).withBasicAuth()

  val basicAuthEndpointWithParameter = endpoint(
    get(path / "users" / segment[Long]("id")),
    ok(textResponse)
  ).withBasicAuth()

  val basicAuthEndpointWithRealm = endpoint(
    get(path / "users"),
    ok(textResponse)
  ).withBasicAuth(Some("Test API"))

  val bearerAuthEndpoint = endpoint(
    get(path / "users"),
    ok(textResponse)
  ).withBearerAuth()

  val bearerAuthEndpointWithParameter = endpoint(
    get(path / "users" / segment[Long]("id")),
    ok(textResponse)
  ).withBearerAuth()

}
