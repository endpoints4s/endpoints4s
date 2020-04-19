package endpoints.http4s.server

import cats.implicits._
import endpoints.algebra.Codec
import endpoints.{Invalid, Valid, algebra}
import fs2.Chunk
import org.http4s
import org.http4s.headers.`Content-Type`
import org.http4s.{EntityEncoder, EntityDecoder, MediaType}

/**
  * Interpreter for [[algebra.JsonEntitiesFromCodecs]] that decodes JSON requests
  * and encodes JSON responses.
  *
  * @group interpreters
  */
trait JsonEntitiesFromCodecs
    extends algebra.JsonEntitiesFromCodecs
    with EndpointsWithCustomErrors {

  /* Setting `strict = true` means that this won't accept requests that are
   * missing their Content-Type header. However, if we use `strict = false`,
   * requests with incorrect specified `Content-Type` still get accepted.
   */
  def jsonRequest[A](implicit codec: JsonCodec[A]): RequestEntity[A] =
    req =>
      http4s.EntityDecoder
        .decodeBy(MediaType.application.json) { msg: http4s.Media[Effect] =>
          http4s.DecodeResult.success(EntityDecoder.decodeString(msg))
        }
        .decode(req, strict = true)
        .leftWiden[Throwable]
        .rethrowT
        .map { value =>
          stringCodec(codec)
            .decode(value) match {
            case Valid(a)     => Right(a)
            case inv: Invalid => Left(handleClientErrors(inv))
          }
        }

  def jsonResponse[A](implicit codec: JsonCodec[A]): ResponseEntity[A] =
    EntityEncoder[Effect, Chunk[Byte]]
      .contramap[A](value =>
        Chunk.bytes(stringCodec(codec).encode(value).getBytes())
      )
      .withContentType(`Content-Type`(MediaType.application.json))

}

/**
  * Interpreter for [[algebra.JsonEntitiesFromSchemas]] that decodes JSON requests
  * and encodes JSON responses.
  *
  * @group interpreters
  */
trait JsonEntitiesFromSchemas
    extends algebra.JsonEntitiesFromSchemas
    with JsonEntitiesFromCodecs
    with endpoints.ujson.JsonSchemas {

  def stringCodec[A](implicit codec: JsonCodec[A]): Codec[String, A] =
    codec.stringCodec

}
