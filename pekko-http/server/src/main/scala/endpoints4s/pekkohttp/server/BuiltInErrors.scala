package endpoints4s.pekkohttp.server

import org.apache.pekko.http.scaladsl.marshalling.Marshaller
import org.apache.pekko.http.scaladsl.model.{HttpEntity, MediaTypes}
import endpoints4s.{Invalid, algebra}

/** @group interpreters
  */
trait BuiltInErrors extends algebra.BuiltInErrors {
  this: EndpointsWithCustomErrors =>

  def clientErrorsResponseEntity: ResponseEntity[Invalid] =
    Marshaller.withFixedContentType(MediaTypes.`application/json`) { invalid =>
      HttpEntity(
        MediaTypes.`application/json`,
        endpoints4s.ujson.codecs.invalidCodec.encode(invalid)
      )
    }

  def serverErrorResponseEntity: ResponseEntity[Throwable] =
    clientErrorsResponseEntity.compose(throwable => Invalid(throwable.getMessage))

}
