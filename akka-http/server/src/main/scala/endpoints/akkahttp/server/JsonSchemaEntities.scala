package endpoints.akkahttp.server

import akka.http.scaladsl.marshalling.Marshaller
import akka.http.scaladsl.model.{HttpEntity, MediaTypes}
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import endpoints.{Invalid, Valid, Validated, algebra}
import ujson.StringRenderer

trait JsonSchemaEntities
  extends algebra.JsonSchemaEntities
    with EndpointsWithCustomErrors
    with endpoints.ujson.JsonSchemas {

  def jsonRequest[A](implicit codec: JsonRequest[A]): RequestEntity[A] = {
    implicit val unmarshaller: FromEntityUnmarshaller[Validated[A]] =
      Unmarshaller.stringUnmarshaller.map(codec.stringCodec.decode)
    Directives.entity[Validated[A]](implicitly).flatMap {
      case Valid(a)     => Directives.provide(a)
      case inv: Invalid => handleClientErrors(inv)
    }
  }

  def jsonResponse[A](implicit codec: JsonResponse[A]): ResponseEntity[A] =
    Marshaller.withFixedContentType(MediaTypes.`application/json`) { value =>
      HttpEntity(
        MediaTypes.`application/json`,
        codec.stringCodec.encode(value)
      )
    }

}
