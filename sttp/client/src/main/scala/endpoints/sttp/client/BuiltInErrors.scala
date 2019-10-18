package endpoints.sttp.client

import endpoints.algebra.InvalidCodec.invalidCodec
import endpoints.{Invalid, algebra}

import scala.language.higherKinds

trait BuiltInErrors[R[_]] extends algebra.BuiltInErrors { this: EndpointsWithCustomErrors[R] =>

  def clientErrorsResponseEntity: ResponseEntity[Invalid] = stringCodecResponse(invalidCodec)

  def serverErrorResponseEntity: ResponseEntity[Throwable] =
    mapResponseEntity(clientErrorsResponseEntity)(invalid => new Throwable(invalid.errors.mkString(". ")))

}
