package sample.algebra

trait DocumentedApi
  extends endpoints.algebra.DocumentedEndpoints
    with endpoints.algebra.DocumentedOptionalResponses {

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

}
