package endpoints4s.akkahttp.client

import endpoints4s.{Invalid, algebra}

/** @group interpreters
  */
trait BuiltInErrors extends algebra.BuiltInErrors {
  this: EndpointsWithCustomErrors =>

  def clientErrorsResponseEntity: ResponseEntity[Invalid] =
    stringCodecResponse(endpoints4s.ujson.codecs.invalidCodec)

  def serverErrorResponseEntity: ResponseEntity[Throwable] =
    mapResponseEntity(clientErrorsResponseEntity)(invalid =>
      new Throwable(invalid.errors.mkString(". "))
    )

}
