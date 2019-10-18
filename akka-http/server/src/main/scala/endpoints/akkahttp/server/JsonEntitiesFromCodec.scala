package endpoints.akkahttp.server

import akka.http.scaladsl.marshalling.Marshaller
import akka.http.scaladsl.model.{HttpEntity, MediaTypes}
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import endpoints.{Invalid, Valid, Validated}
import endpoints.algebra.Codec

/**
  * Interpreter for [[endpoints.algebra.JsonEntitiesFromCodec]] that decodes JSON requests and
  * encodes JSON responses using Akka HTTP.
  *
  * @group interpreters
  */
trait JsonEntitiesFromCodec extends EndpointsWithCustomErrors with endpoints.algebra.JsonEntitiesFromCodec {

  def jsonRequest[A](implicit codec: Codec[String, A]): RequestEntity[A] = {
    implicit val fromEntityUnmarshaller: FromEntityUnmarshaller[Validated[A]] =
      Unmarshaller.stringUnmarshaller
        .forContentTypes(MediaTypes.`application/json`)
        .map(data => codec.decode(data))
    Directives.entity[Validated[A]](implicitly).flatMap {
      case Valid(a)     => Directives.provide(a)
      case inv: Invalid => handleClientErrors(inv)
    }
  }

  def jsonResponse[A](implicit codec: Codec[String, A]): ResponseEntity[A] =
    Marshaller.withFixedContentType(MediaTypes.`application/json`) { value =>
      HttpEntity(MediaTypes.`application/json`, codec.encode(value))
    }

}
