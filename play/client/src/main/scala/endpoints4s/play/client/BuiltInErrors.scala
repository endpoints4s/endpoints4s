package endpoints4s.play.client

import endpoints4s.{Invalid, algebra}

/**
  * @group interpreters
  */
trait BuiltInErrors extends algebra.BuiltInErrors {
  this: EndpointsWithCustomErrors =>

  def clientErrorsResponseEntity: ResponseEntity[Invalid] =
    wsResp =>
      endpoints4s.ujson.codecs.invalidCodec
        .decode(wsResp.body)
        .fold(Right(_), errors => Left(new Exception(errors.mkString(". "))))

  def serverErrorResponseEntity: ResponseEntity[Throwable] =
    mapResponseEntity(clientErrorsResponseEntity)(invalid =>
      new Throwable(invalid.errors.mkString(". "))
    )

}
