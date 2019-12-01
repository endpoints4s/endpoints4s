package endpoints.sttp.client

/**
  * Interpreter for [[endpoints.algebra.JsonEntitiesFromCodec]] that encodes JSON request
  * @group interpreters
  */
trait JsonEntitiesFromCodec[R[_]] extends endpoints.algebra.JsonEntitiesFromCodec { self: EndpointsWithCustomErrors[R] =>

  def jsonRequest[A](implicit codec: JsonCodec[A]): RequestEntity[A] = { (a, req) =>
    req.body(stringCodec(codec).encode(a)).contentType("application/json")
  }

  def jsonResponse[A](implicit codec: JsonCodec[A]): ResponseEntity[A] = stringCodecResponse(stringCodec(codec))

}
