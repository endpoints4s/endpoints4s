package endpoints.scalaj.client
import endpoints.algebra.{Codec, Documentation}

/**
  * Interpreter for [[endpoints.algebra.JsonEntitiesFromCodec]] that encodes JSON requests
  * and decodes JSON responses.
  */
trait JsonEntitiesFromCodec extends endpoints.algebra.JsonEntitiesFromCodec {

  override val endpoints: Endpoints

  import endpoints._
  import requests._
  import responses._

  def jsonRequest[A](docs: Documentation)(implicit codec: Codec[String, A]): RequestEntity[A] = (data, request) => {
    request.header("Content-Type", "application/json")
    request.postData(codec.encode(data))
  }

  def jsonResponse[A](docs: Documentation)(implicit codec: Codec[String, A]): Response[A] =
    resp => codec.decode(resp.body)

}
