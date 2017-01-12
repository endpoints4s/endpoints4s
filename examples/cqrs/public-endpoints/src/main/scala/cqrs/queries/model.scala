package cqrs.queries

import java.time.OffsetDateTime

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.java8.time._

import scala.collection.immutable.SortedMap

/**
  * This is the model used for querying.
  */
// TODO Add useful stats
case class Meter(label: String, timeSeries: SortedMap[OffsetDateTime, BigDecimal])

object Meter {

  implicit def decodeSortedMap[A : Decoder : Ordering, B : Decoder]: Decoder[SortedMap[A, B]] =
    Decoder[Seq[(A, B)]].map(entries => (SortedMap.newBuilder[A, B] ++= entries).result())

  implicit def encodeSortedMap[A : Encoder, B : Encoder]: Encoder[SortedMap[A, B]] =
    Encoder.encodeList[(A, B)].contramap[SortedMap[A, B]](_.to[List])

  implicit val decoder: Decoder[Meter] = deriveDecoder
  implicit val encoder: Encoder[Meter] = deriveEncoder

}
