package endpoints.xhr

import endpoints.{Invalid, algebra}

/**
  * @group interpreters
  */
trait BuiltInErrors extends algebra.BuiltInErrors { this: EndpointsWithCustomErrors =>

  def clientErrorsResponseEntity: ResponseEntity[Invalid] =
    stringCodecResponse(endpoints.ujson.codecs.invalidCodec)

  def serverErrorResponseEntity: ResponseEntity[Throwable] =
    mapResponseEntity(clientErrorsResponseEntity)(invalid => new Throwable(invalid.errors.mkString(". ")))

}
