package cqrs

import endpoints.algebra.{CirceEntities, Endpoints}
import io.circe.Json

trait QueryEndpoints extends Endpoints with CirceEntities {

  /**
    * This is our *internal* protocol for queries. We don’t have to suffer from
    * REST dictatorship:
    *  - our client doesn’t care about the “semantic” difference between POST and GET.
    *  - status codes other than 500 and 200 are useless: the query is built
    *    via a statically typed API, so we can not build bad requests, by construction, and the response
    *    entity gives way more details about failures than status codes.
    */
  val query: MuxEndpoint[Query, QueryResult, Json] =
    muxEndpoint[Query, QueryResult, Json](post[Unit, Json, Unit, Json](path / "query", jsonRequest[Json]), jsonResponse[Json])

}
