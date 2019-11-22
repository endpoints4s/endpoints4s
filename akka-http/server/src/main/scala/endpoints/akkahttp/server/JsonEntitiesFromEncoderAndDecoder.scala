package endpoints.akkahttp.server

import akka.http.scaladsl.marshalling.Marshaller
import akka.http.scaladsl.model.{HttpEntity, MediaTypes}
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import endpoints.{Invalid, Valid, Validated}
import endpoints.algebra.{Decoder, Encoder}

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

  type JsonRequest[A] = Decoder[String, A]
  type JsonResponse[A] = Encoder[A, String]

  def jsonRequest[A](implicit codec: Decoder[String, A]): RequestEntity[A] = {
    implicit val fromEntityUnmarshaller: FromEntityUnmarshaller[Validated[A]] =
      Unmarshaller.stringUnmarshaller
        .forContentTypes(MediaTypes.`application/json`)
        .map(data => codec.decode(data))
    Directives.entity[Validated[A]](implicitly).flatMap {
      case Valid(a)     => Directives.provide(a)
      case inv: Invalid => handleClientErrors(inv)
    }
  }

  def jsonResponse[A](implicit codec: Encoder[A, String]): ResponseEntity[A] =
    Marshaller.withFixedContentType(MediaTypes.`application/json`) { value =>
      HttpEntity(MediaTypes.`application/json`, codec.encode(value))
    }

}
