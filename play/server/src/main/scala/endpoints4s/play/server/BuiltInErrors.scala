package endpoints4s.play.server

import endpoints4s.{Invalid, algebra}
import play.api.http.{ContentTypes, Writeable}

/**
  * @group interpreters
  */
trait BuiltInErrors extends algebra.BuiltInErrors {
  this: EndpointsWithCustomErrors =>

  def clientErrorsResponseEntity: ResponseEntity[Invalid] =
    responseEntityFromWriteable({
      val playCodec = implicitly[play.api.mvc.Codec]
      Writeable(
        (invalid: Invalid) =>
          playCodec.encode(
            endpoints4s.ujson.codecs.invalidCodec.encode(invalid)
          ),
        Some(ContentTypes.JSON)
      )
    })

  def serverErrorResponseEntity: ResponseEntity[Throwable] =
    throwable => clientErrorsResponseEntity(Invalid(throwable.getMessage))

}
