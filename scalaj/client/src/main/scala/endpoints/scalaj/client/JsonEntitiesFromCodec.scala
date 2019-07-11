package endpoints.scalaj.client
import endpoints.algebra.Documentation

/**
  * Interpreter for [[endpoints.algebra.JsonEntitiesFromCodec]] that encodes JSON requests
  * and decodes JSON responses.
  *
  * @group interpreters
  */
trait JsonEntitiesFromCodec extends Endpoints with endpoints.algebra.JsonEntitiesFromCodec {

  def jsonRequest[A](docs: Documentation)(implicit codec: JsonCodec[A]): RequestEntity[A] = (data, request) => {
    request.header("Content-Type", "application/json")
    request.postData(jsonCodecToCodec(codec).encode(data))
  }

  def jsonResponse[A](docs: Documentation)(implicit codec: JsonCodec[A]): Response[A] =
    resp => jsonCodecToCodec(codec).decode(resp.body)

}
