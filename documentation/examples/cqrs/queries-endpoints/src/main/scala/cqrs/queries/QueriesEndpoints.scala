package cqrs.queries

import java.util.UUID

import endpoints4s.algebra.{BuiltInErrors, MuxEndpoints, MuxRequest, circe}
import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** This is our *internal* protocol for queries. We don’t have to suffer from
  * REST conventions:
  *  - our client doesn’t care about the “semantic” difference between POST and GET.
  *  - status codes other than 500 and 200 are useless: the query is built
  *    via a statically typed API, so we can not build bad requests, by construction, and the response
  *    entity gives way more details about failures than status codes.
  */
//#mux-endpoint
trait QueriesEndpoints extends MuxEndpoints with BuiltInErrors with circe.JsonEntitiesFromCodecs {

  val query: MuxEndpoint[QueryReq, QueryResp, Json] = {
    val request = post(path / "query", jsonRequest[Json])
    muxEndpoint(request, ok(jsonResponse[Json]))
  }

}
//#mux-endpoint

/** A request carrying a query */
//#mux-requests
sealed trait QueryReq extends MuxRequest
final case class FindById(id: UUID, after: Option[Long]) extends QueryReq {
  type Response = MaybeResource
}
final case object FindAll extends QueryReq { type Response = ResourceList }
//#mux-requests
// TODO Add a type of query including complex filters

object QueryReq {
  implicit val queryDecoder: Decoder[QueryReq] = deriveDecoder
  implicit val queryEncoder: Encoder[QueryReq] = deriveEncoder
}

/** A response to a QueryReq */
// TODO Enrich with failure information
//#mux-responses
sealed trait QueryResp
case class MaybeResource(value: Option[Meter]) extends QueryResp
case class ResourceList(value: List[Meter]) extends QueryResp
//#mux-responses

object QueryResp {
  implicit val queryDecoder: Decoder[QueryResp] = deriveDecoder
  implicit val queryEncoder: Encoder[QueryResp] = deriveEncoder
}
