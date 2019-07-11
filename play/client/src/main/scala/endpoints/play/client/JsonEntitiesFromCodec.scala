package endpoints.play.client

import endpoints.algebra.Documentation
import play.api.http.ContentTypes
import play.api.libs.ws.{BodyWritable, InMemoryBody}

/**
  * Interpreter for [[endpoints.algebra.JsonEntitiesFromCodec]] that encodes JSON requests
  * and decodes JSON responses.
  *
  * @group interpreters
  */
trait JsonEntitiesFromCodec extends Endpoints with endpoints.algebra.JsonEntitiesFromCodec {

  def jsonRequest[A](docs: Documentation)(implicit codec: JsonCodec[A]): RequestEntity[A] = { (a, wsRequest) =>
    val playCodec: play.api.mvc.Codec = implicitly[play.api.mvc.Codec]
    val writable = BodyWritable((s: String) => InMemoryBody(playCodec.encode(s)), ContentTypes.JSON)
    wsRequest.withBody(jsonCodecToCodec(codec).encode(a))(writable)
  }

  def jsonResponse[A](docs: Documentation)(implicit codec: JsonCodec[A]): Response[A] =
    response => jsonCodecToCodec(codec).decode(response.body)

}
