package sample.algebra

trait DocumentedApi
  extends endpoints.algebra.DocumentedEndpoints
    with endpoints.algebra.DocumentedOptionalResponses
    with endpoints.algebra.DocumentedBasicAuthentication {

  val items =
    endpoint(
      get(path / "items" / segment[String]("category") /? optQs[Int]("page")),
      emptyResponse("List all the items of the given category")
    )

  val itemId = segment[String]("id")

  val item =
    endpoint(
      get(path / "item" / itemId),
      option(emptyResponse("The item identified by 'id'"), "Item not found")
    )

  val admin =
    authenticatedEndpoint(
      Get,
      path / "admin",
      emptyRequest,
      emptyResponse("Administration page"),
      "Authentication error"
    )

}
