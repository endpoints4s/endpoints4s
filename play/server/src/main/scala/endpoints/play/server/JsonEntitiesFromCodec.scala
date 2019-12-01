package endpoints.play.server

import endpoints.{Invalid, Valid}
import play.api.http.{ContentTypes, Writeable}

/**
  * Interpreter for [[endpoints.algebra.JsonEntitiesFromCodec]] that decodes JSON requests
  * and encodes JSON responses.
  *
  * @group interpreters
  */
trait JsonEntitiesFromCodec extends EndpointsWithCustomErrors with endpoints.algebra.JsonEntitiesFromCodec {

  import playComponents.executionContext

  def jsonRequest[A](implicit codec: JsonCodec[A]): RequestEntity[A] =
    playComponents.playBodyParsers.tolerantText.validate { body =>
      stringCodec(codec).decode(body) match {
        case Valid(value) => Right(value)
        case inv: Invalid => Left(handleClientErrors(inv))
      }
    }

  def jsonResponse[A](implicit codec: JsonCodec[A]): ResponseEntity[A] = {
    val playCodec = implicitly[play.api.mvc.Codec]
    Writeable((a: A) => playCodec.encode(stringCodec(codec).encode(a)), Some(ContentTypes.JSON))
  }

}
