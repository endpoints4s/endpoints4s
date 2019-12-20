package endpoints.play.server

import endpoints.{Invalid, algebra}
import play.api.http.{ContentTypes, Writeable}

/**
  * @group interpreters
  */
trait BuiltInErrors extends algebra.BuiltInErrors { this: EndpointsWithCustomErrors =>

  def clientErrorsResponseEntity: ResponseEntity[Invalid] = {
    val playCodec = implicitly[play.api.mvc.Codec]
    Writeable(invalid => playCodec.encode(endpoints.ujson.codecs.invalidCodec.encode(invalid)), Some(ContentTypes.JSON))
  }

  def serverErrorResponseEntity: ResponseEntity[Throwable] =
    clientErrorsResponseEntity.map(throwable => Invalid(throwable.getMessage))

}
