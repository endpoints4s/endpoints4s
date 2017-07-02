package sample.algebra

import endpoints.documented.algebra.{BasicAuthentication, Endpoints, CirceEntities, OptionalResponses}
import io.circe.generic.JsonCodec

trait DocumentedApi
  extends Endpoints
    with OptionalResponses
    with BasicAuthentication
    with CirceEntities {

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

}

@JsonCodec
case class Item(name: String)
