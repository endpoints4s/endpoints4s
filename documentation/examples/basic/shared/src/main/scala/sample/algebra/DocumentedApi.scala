package sample.algebra

import endpoints4s.algebra.{BasicAuthentication, circe, Endpoints}
import io.circe.generic.JsonCodec

trait DocumentedApi extends Endpoints with BasicAuthentication with circe.JsonEntitiesFromCodecs {

  val items =
    endpoint(
      get(
        path / "items" / segment[String]("category") /? qs[Option[Int]]("page")
      ),
      ok(
        jsonResponse[List[Item]],
        Some("List all the items of the given category")
      )
    )

  val itemId = segment[String]("id")

  val item =
    endpoint(
      get(path / "item" / itemId),
      wheneverFound(
        ok(jsonResponse[Item], Some("The item identified by 'id'")),
        Some("Item not found")
      )
    )

  val admin =
    authenticatedEndpoint(
      Get,
      path / "admin",
      requestEntity = emptyRequest,
      response = ok(emptyResponse, Some("Administration page")),
      endpointDocs = EndpointDocs().withSummary(Some("Authentication endpoint"))
    )

}

@JsonCodec
case class Item(name: String)
