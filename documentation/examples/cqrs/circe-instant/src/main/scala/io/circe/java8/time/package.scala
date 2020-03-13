package io.circe.java8

import java.time.Instant

import io.circe.{Decoder, Encoder}

// Custom version because Scala.js does not support Instant.parse yet
package object time {

  implicit final val decodeInstant: Decoder[Instant] =
    Decoder[(Long, Long)]
      .map { case (seconds, nanos) => Instant.ofEpochSecond(seconds, nanos) }

  implicit final val encodeInstant: Encoder[Instant] =
    Encoder[(Long, Long)]
      .contramap[Instant](instant =>
        (instant.getEpochSecond, instant.getNano.toLong)
      )

}
