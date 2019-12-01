package endpoints.play.server

import endpoints.{Invalid, Valid, algebra}
import play.api.http.{ContentTypes, Writeable}

trait JsonSchemaEntities
  extends algebra.JsonSchemaEntities
    with EndpointsWithCustomErrors
    with endpoints.ujson.JsonSchemas {

  import playComponents.executionContext

  def jsonRequest[A](implicit codec: JsonRequest[A]): RequestEntity[A] =
    playComponents.playBodyParsers.tolerantText.validate { body =>
      codec.stringCodec.decode(body) match {
        case Valid(value) => Right(value)
        case inv: Invalid => Left(handleClientErrors(inv))
      }
    }

  def jsonResponse[A](implicit codec: JsonResponse[A]): ResponseEntity[A] = {
    val playCodec = implicitly[play.api.mvc.Codec]
    Writeable(
      (a: A) => playCodec.encode(codec.stringCodec.encode(a)),
      Some(ContentTypes.JSON)
    )
  }


}
