package endpoints4s.sttp.client

import endpoints4s.{Invalid, algebra}

/** @group interpreters
  */
trait BuiltInErrors[R[_]] extends algebra.BuiltInErrors {
  this: EndpointsWithCustomErrors[R] =>

  def clientErrorsResponseEntity: ResponseEntity[Invalid] =
    stringCodecResponse(endpoints4s.ujson.codecs.invalidCodec)

  def serverErrorResponseEntity: ResponseEntity[Throwable] =
    mapResponseEntity(clientErrorsResponseEntity)(invalid =>
      new Throwable(invalid.errors.mkString(". "))
    )

}
