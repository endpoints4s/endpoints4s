package endpoints.play.client

import endpoints.algebra.InvalidCodec.invalidCodec
import endpoints.{Invalid, algebra}

trait BuiltInErrors extends algebra.BuiltInErrors { this: EndpointsWithCustomErrors =>

  def clientErrorsResponseEntity: ResponseEntity[Invalid] =
    wsResp => invalidCodec.decode(wsResp.body).fold(Right(_), errors => Left(new Exception(errors.mkString(". "))))

  def serverErrorResponseEntity: ResponseEntity[Throwable] =
    mapResponseEntity(clientErrorsResponseEntity)(invalid => new Throwable(invalid.errors.mkString(". ")))

}
