package sample.algebra

import endpoints.algebra.{BasicAuthentication, circe, Endpoints}
import io.circe.generic.JsonCodec

trait DocumentedApi
  extends Endpoints
    with BasicAuthentication
    with circe.JsonEntitiesFromCodec {

  val items =
    endpoint(
      get(path / "items" / segment[String]("category") /? qs[Option[Int]]("page")),
      jsonResponse[List[Item]](Some("List all the items of the given category"))
    )

  val itemId = segment[String]("id")

  val item =
    endpoint(
      get(path / "item" / itemId),
      wheneverFound(jsonResponse[Item](Some("The item identified by 'id'")), Some("Item not found"))
    )

  val admin =
    authenticatedEndpoint(
      Get,
      path / "admin",
      requestEntity = emptyBody,
      response = emptyResponse(Some("Administration page")),
      summary = Some("Authentication endpoint")
    )

}

@JsonCodec
case class Item(name: String)
