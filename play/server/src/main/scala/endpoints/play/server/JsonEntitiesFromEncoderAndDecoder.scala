package endpoints.play.server

import akka.util.ByteString
import endpoints.algebra.{Decoder, Encoder}
import play.api.http.{ContentTypes, Writeable}

/**
  * Interpreter for [[endpoints.algebra.JsonEntities]] that decodes JSON entities with a
  * [[endpoints.algebra.Decoder]] and encodes JSON entities with an [[endpoints.algebra.Encoder]].
  *
  * The difference with [[JsonEntitiesFromCodec]] is that you don’t need bidirectional codecs:
  * you only need an encoder to build responses, or a decoder to decode requests.
  *
  * It is especially useful to encode `OpenApi` documents into JSON entities.
  *
  * @group interpreter
  */
trait JsonEntitiesFromEncoderAndDecoder extends EndpointsWithCustomErrors with endpoints.algebra.JsonEntities {

  type JsonResponse[A] = Encoder[A, String]
  type JsonRequest[A]  = Decoder[String, A]

  def jsonRequest[A](implicit decoder: JsonRequest[A]): RequestEntity[A] =
    playComponents.playBodyParsers.byteString.validate { bs =>
      decoder.decode(bs.utf8String).toEither.left.map(errs => handleClientErrors(endpoints.Invalid(errs)))
    }(playComponents.executionContext)

  def jsonResponse[A](implicit encoder: JsonResponse[A]): ResponseEntity[A] =
    Writeable(a => ByteString(encoder.encode(a)), Some(ContentTypes.JSON))

}
