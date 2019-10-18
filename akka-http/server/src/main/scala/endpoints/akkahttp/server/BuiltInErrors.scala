package endpoints.akkahttp.server

import akka.http.scaladsl.marshalling.Marshaller
import akka.http.scaladsl.model.{HttpEntity, MediaTypes}
import endpoints.algebra.InvalidCodec.invalidCodec
import endpoints.{Invalid, algebra}

trait BuiltInErrors extends algebra.BuiltInErrors { this: EndpointsWithCustomErrors =>

  def clientErrorsResponseEntity: ResponseEntity[Invalid] =
    Marshaller.withFixedContentType(MediaTypes.`application/json`) { invalid =>
      HttpEntity(MediaTypes.`application/json`, invalidCodec.encode(invalid))
    }

  def serverErrorResponseEntity: ResponseEntity[Throwable] =
    clientErrorsResponseEntity.compose(throwable => Invalid(throwable.getMessage))

}
