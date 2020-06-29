package endpoints4s.scalaj.client

import endpoints4s.{Invalid, algebra}

/**
  * @group interpreters
  */
trait BuiltInErrors extends algebra.BuiltInErrors {
  this: EndpointsWithCustomErrors =>

  def clientErrorsResponseEntity: ResponseEntity[Invalid] =
    resp =>
      endpoints4s.ujson.codecs.invalidCodec
        .decode(resp)
        .fold(Right(_), errors => Left(new Exception(errors.mkString(". "))))

  def serverErrorResponseEntity: ResponseEntity[Throwable] =
    resp =>
      clientErrorsResponseEntity(resp).map(invalid =>
        new Throwable(invalid.errors.mkString(". "))
      )

}
