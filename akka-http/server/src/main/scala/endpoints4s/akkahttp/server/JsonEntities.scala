package endpoints4s.akkahttp.server

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.{HttpEntity, MediaTypes}
import akka.http.scaladsl.server.{Directive1, Directives}
import akka.http.scaladsl.unmarshalling.{
  FromEntityUnmarshaller,
  FromRequestUnmarshaller,
  Unmarshaller
}
import endpoints4s.{Codec, Decoder, Encoder, Invalid, Valid, Validated, algebra}

/** Interpreter for [[algebra.JsonEntities]]
  *
  * To use it mix in support for your favourite Json library
  * You can use one of [[https://github.com/hseeberger/akka-http-json hseeberger/akka-http-json]] modules
  *
  * @group interpreters
  */
trait JsonEntities extends algebra.JsonEntities with EndpointsWithCustomErrors {

  type JsonRequest[A] = FromRequestUnmarshaller[A]

  def jsonRequest[A: JsonRequest]: RequestEntity[A] =
    Directives.entity[A](implicitly)

  type JsonResponse[A] = ToEntityMarshaller[A]

  def jsonResponse[A: JsonResponse]: ResponseEntity[A] =
    implicitly

}

/** Interpreter for [[algebra.JsonEntitiesFromCodecs]] that decodes JSON requests and
  * encodes JSON responses using Akka HTTP.
  *
  * @group interpreters
  */
trait JsonEntitiesFromCodecs extends algebra.JsonEntitiesFromCodecs with EndpointsWithCustomErrors {

  def jsonRequest[A](implicit codec: JsonCodec[A]): RequestEntity[A] =
    JsonEntities.decodeJsonRequest(this)(stringCodec(codec))

  def jsonResponse[A](implicit codec: JsonCodec[A]): ResponseEntity[A] =
    JsonEntities.encodeJsonResponse(stringCodec(codec))

}

/** Interpreter for [[algebra.JsonEntitiesFromSchemas]] that decodes JSON requests and
  * encodes JSON responses using Akka HTTP.
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
    JsonEntities.encodeJsonResponse(encoder)

}

private object JsonEntities {

  def decodeJsonRequest[A](
      endpoints: EndpointsWithCustomErrors
  )(decoder: Decoder[String, A]): Directive1[A] = {
    implicit val fromEntityUnmarshaller: FromEntityUnmarshaller[Validated[A]] =
      Unmarshaller.stringUnmarshaller
        .forContentTypes(MediaTypes.`application/json`)
        .map(data => decoder.decode(data))
    Directives.entity[Validated[A]](implicitly).flatMap {
      case Valid(a)     => Directives.provide(a)
      case inv: Invalid => endpoints.handleClientErrors(inv)
    }
  }

  def encodeJsonResponse[A](
      encoder: Encoder[A, String]
  ): ToEntityMarshaller[A] =
    Marshaller.withFixedContentType(MediaTypes.`application/json`) { value =>
      HttpEntity(MediaTypes.`application/json`, encoder.encode(value))
    }

}
