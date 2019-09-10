package endpoints.play.server

import endpoints.algebra.Codec
import play.api.http.{ContentTypes, Writeable}
import play.api.mvc.Results

/**
  * Interpreter for [[endpoints.algebra.JsonEntitiesFromCodec]] that decodes JSON requests
  * and encodes JSON responses.
  *
  * @group interpreters
  */
trait JsonEntitiesFromCodec extends Endpoints with endpoints.algebra.JsonEntitiesFromCodec {

  import playComponents.executionContext

  def jsonRequest[A](implicit codec: Codec[String, A]): RequestEntity[A] =
    playComponents.playBodyParsers.tolerantText.validate { body =>
      codec.decode(body).left.map(ignoredError => Results.BadRequest)
    }

  def jsonResponse[A](implicit codec: Codec[String, A]): ResponseEntity[A] = {
    val playCodec = implicitly[play.api.mvc.Codec]
    Writeable((a: A) => playCodec.encode(codec.encode(a)), Some(ContentTypes.JSON))
  }

}
