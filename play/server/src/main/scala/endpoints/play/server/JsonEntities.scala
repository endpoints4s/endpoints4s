package endpoints.play.server

import akka.util.ByteString
import endpoints.algebra
import endpoints.algebra.{Decoder, Encoder, Codec}
import endpoints.Invalid
import play.api.mvc.RequestHeader
import play.api.http.{ContentTypes, Writeable}

/**
  * Interpreter for [[algebra.JsonEntitiesFromCodecs]] that decodes JSON requests
  * and encodes JSON responses.
  *
  * @group interpreters
  */
trait JsonEntitiesFromCodecs
    extends algebra.JsonEntitiesFromCodecs
    with EndpointsWithCustomErrors {

  def jsonRequest[A](implicit codec: JsonCodec[A]): RequestEntity[A] =
    JsonEntities.decodeRequest(this)(stringCodec(codec))

  def jsonResponse[A](implicit codec: JsonCodec[A]): ResponseEntity[A] =
    responseEntityFromWriteable(JsonEntities.encodeResponse(stringCodec(codec)))

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

/**
  * Interpreter for [[algebra.JsonEntities]] that decodes JSON entities with a
  * [[algebra.Decoder]] and encodes JSON entities with an [[algebra.Encoder]].
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

  type JsonResponse[A] = Encoder[A, String]
  type JsonRequest[A] = Decoder[String, A]

  def jsonRequest[A](implicit decoder: JsonRequest[A]): RequestEntity[A] =
    JsonEntities.decodeRequest(this)(decoder)

  def jsonResponse[A](implicit encoder: JsonResponse[A]): ResponseEntity[A] =
    responseEntityFromWriteable(JsonEntities.encodeResponse(encoder))

}

private object JsonEntities {

  def decodeRequest[A](
      endpoints: EndpointsWithCustomErrors
  )(decoder: Decoder[String, A]): endpoints.RequestEntity[A] =
    (request: RequestHeader) => {
      if (request.contentType.exists(_.equalsIgnoreCase("application/json"))) {
        val decodeJson = (bs: ByteString) =>
          decoder
            .decode(bs.utf8String)
            .toEither
            .left
            .map(errs => endpoints.handleClientErrors(Invalid(errs)))

        val bodyParser =
          endpoints.playComponents.playBodyParsers.byteString
            .validate(decodeJson)(endpoints.playComponents.executionContext)
        Some(bodyParser)
      } else {
        None
      }
    }

  def encodeResponse[A](encoder: Encoder[A, String]): Writeable[A] =
    Writeable(a => ByteString(encoder.encode(a)), Some(ContentTypes.JSON))

}
