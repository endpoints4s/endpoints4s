package cqrs

import endpoints.algebra.MuxRequest
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class Resource(version: Long, id: Long)

object Resource {
  implicit val resourceDecoder: Decoder[Resource] = deriveDecoder
  implicit val resourceEncoder: Encoder[Resource] = deriveEncoder
}

sealed trait Query extends MuxRequest
final case class FindById(id: Long) extends Query { type Response = MaybeResource }
final case object FindAll extends Query { type Response = ResourceList }
final case class Find(/* TODO Some search criterias */) extends Query { type Response = ResourceList }

object Query {
  implicit val queryDecoder: Decoder[Query] = deriveDecoder
  implicit val queryEncoder: Encoder[Query] = deriveEncoder
}

// TODO Enrich with failure information
sealed trait QueryResult
case class MaybeResource(value: Option[Resource]) extends QueryResult
case class ResourceList(value: List[Resource]) extends QueryResult

object QueryResult {
  implicit val queryDecoder: Decoder[QueryResult] = deriveDecoder
  implicit val queryEncoder: Encoder[QueryResult] = deriveEncoder
}
