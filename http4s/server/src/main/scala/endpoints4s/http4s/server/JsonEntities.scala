package endpoints4s.http4s.server

import endpoints4s.{Codec, Decoder, Encoder, Invalid, Valid, algebra}
import fs2.Chunk
import org.http4s
import org.http4s.headers.`Content-Type`
import org.http4s.{EntityDecoder, EntityEncoder, MediaType}

/** Interpreter for [[algebra.JsonEntitiesFromCodecs]] that decodes JSON requests
  * and encodes JSON responses.
  *
  * @group interpreters
  */
trait JsonEntitiesFromCodecs extends algebra.JsonEntitiesFromCodecs with EndpointsWithCustomErrors {

  /* Setting `strict = true` means that this won't accept requests that are
   * missing their Content-Type header. However, if we use `strict = false`,
   * requests with incorrect specified `Content-Type` still get accepted.
   */
  def jsonRequest[A](implicit codec: JsonCodec[A]): RequestEntity[A] =
    JsonEntities.decodeJsonRequest(this)(stringCodec)

  def jsonResponse[A](implicit codec: JsonCodec[A]): ResponseEntity[A] =
    JsonEntities.encodeJsonResponse(this)(stringCodec)
}

/** Interpreter for [[algebra.JsonEntitiesFromSchemas]] that decodes JSON requests
  * and encodes JSON responses.
  *
  * @group interpreters
  */
trait JsonEntitiesFromSchemas
    extends algebra.JsonEntitiesFromSchemas
    with JsonEntitiesFromCodecs
    with endpoints4s.ujson.JsonSchemas {

  def stringCodec[A](implicit codec: JsonCodec[A]): Codec[String, A] =
    codec.stringCodec

}

/** Interpreter for [[endpoints4s.algebra.JsonEntities]] that decodes JSON entities with a
  * [[endpoints4s.Decoder]] and encodes JSON entities with an [[endpoints4s.Encoder]].
  *
  * The difference with [[JsonEntitiesFromCodecs]] is that you don’t need bidirectional codecs:
  * you only need an encoder to build responses, or a decoder to decode requests.
  *
  * It is especially useful to encode `OpenApi` documents into JSON entities.
  *
  * @group interpreters
  */
trait JsonEntitiesFromEncodersAndDecoders
    extends algebra.JsonEntities
    with EndpointsWithCustomErrors {

  type JsonRequest[A] = Decoder[String, A]
  type JsonResponse[A] = Encoder[A, String]

  def jsonRequest[A](implicit decoder: Decoder[String, A]): RequestEntity[A] =
    JsonEntities.decodeJsonRequest(this)(decoder)

  def jsonResponse[A](implicit encoder: Encoder[A, String]): ResponseEntity[A] =
    JsonEntities.encodeJsonResponse(this)(encoder)
}

private object JsonEntities {

  def decodeJsonRequest[A](
      endpoints: EndpointsWithCustomErrors
  )(decoder: Decoder[String, A]): endpoints.RequestEntity[A] = {
    val entityDecoder = http4s.EntityDecoder
      .decodeBy(MediaType.application.json) { (msg: http4s.Media[endpoints.Effect]) =>
        http4s.DecodeResult.success(EntityDecoder.decodeText(msg)(endpoints.Effect))(
          endpoints.Effect
        )
      }(endpoints.Effect)
    req => {
      val decodeResult = entityDecoder
        .decode(req, strict = true)
        .leftMap[Throwable](identity)(endpoints.Effect)
        .rethrowT(endpoints.Effect)
      endpoints.Effect.flatMap(decodeResult) { value =>
        decoder.decode(value) match {
          case Valid(a) => endpoints.Effect.pure(Right(a))
          case inv: Invalid =>
            endpoints.Effect.map(endpoints.handleClientErrors(inv))(Left.apply)
        }
      }
    }
  }

  def encodeJsonResponse[A](
      endpoints: EndpointsWithCustomErrors
  )(encoder: Encoder[A, String]): endpoints.ResponseEntity[A] =
    EntityEncoder[endpoints.Effect, Chunk[Byte]]
      .contramap[A](value => Chunk.bytes(encoder.encode(value).getBytes()))
      .withContentType(`Content-Type`(MediaType.application.json))
}
