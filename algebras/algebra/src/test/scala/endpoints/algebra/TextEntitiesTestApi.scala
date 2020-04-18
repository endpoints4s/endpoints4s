package endpoints.algebra

trait TextEntitiesTestApi extends EndpointsTestApi {

  val textRequestEndpointTest: Endpoint[String, String] =
    endpoint(
      post(path / "text", textRequest),
      ok(textResponse)
    )
}
