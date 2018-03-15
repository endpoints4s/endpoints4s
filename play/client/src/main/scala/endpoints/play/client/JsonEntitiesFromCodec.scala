package endpoints.play.client

import endpoints.algebra.Codec
import play.api.http.ContentTypes
import play.api.libs.ws.{BodyWritable, InMemoryBody}

/**
  * Interpreter for [[endpoints.algebra.JsonEntitiesFromCodec]] that encodes JSON requests
  * and decodes JSON responses.
  */
trait JsonEntitiesFromCodec extends Endpoints with endpoints.algebra.JsonEntitiesFromCodec {

  def jsonRequest[A](implicit codec: Codec[String, A]): RequestEntity[A] = { (a, wsRequest) =>
    val playCodec: play.api.mvc.Codec = implicitly[play.api.mvc.Codec]
    val writable = BodyWritable((s: String) => InMemoryBody(playCodec.encode(s)), ContentTypes.JSON)
    wsRequest.withBody(codec.encode(a))(writable)
  }

  def jsonResponse[A](implicit codec: Codec[String, A]): Response[A] =
    response => codec.decode(response.body)

}
