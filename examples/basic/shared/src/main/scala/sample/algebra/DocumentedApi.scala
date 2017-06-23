package sample.algebra

import io.circe.generic.JsonCodec

trait DocumentedApi
  extends endpoints.algebra.DocumentedEndpoints
    with endpoints.algebra.DocumentedOptionalResponses
    with endpoints.algebra.DocumentedBasicAuthentication
    with endpoints.algebra.DocumentedJsonEntities {

  val items =
    endpoint(
      get(path / "items" / segment[String]("category") /? optQs[Int]("page")),
      jsonResponse[List[Item]]("List all the items of the given category")
    )

  val itemId = segment[String]("id")

  val item =
    endpoint(
      get(path / "item" / itemId),
      option(jsonResponse[Item]("The item identified by 'id'"), "Item not found")
    )

  val admin =
    authenticatedEndpoint(
      Get,
      path / "admin",
      emptyRequest,
      emptyResponse("Administration page"),
      "Authentication error"
    )

  implicit def itemDecoder: JsonResponse[Item]
  implicit def listDecoder[A : JsonResponse]: JsonResponse[List[A]]

}

@JsonCodec
case class Item(name: String)
