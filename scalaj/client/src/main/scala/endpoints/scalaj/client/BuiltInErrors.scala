package endpoints.scalaj.client

import endpoints.algebra.InvalidCodec.invalidCodec
import endpoints.{Invalid, algebra}

trait BuiltInErrors extends algebra.BuiltInErrors { this: EndpointsWithCustomErrors =>

  def clientErrorsResponseEntity: ResponseEntity[Invalid] =
    resp => invalidCodec.decode(resp).fold(Right(_), errors => Left(new Exception(errors.mkString(". "))))

  def serverErrorResponseEntity: ResponseEntity[Throwable] =
    resp => clientErrorsResponseEntity(resp).right.map(invalid => new Throwable(invalid.errors.mkString(". ")))

}
