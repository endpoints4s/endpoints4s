package endpoints4s.sttp.client

/** Interpreter for [[endpoints4s.algebra.JsonEntitiesFromCodecs]] that encodes JSON request
  *
  * @group interpreters
  */
trait JsonEntitiesFromCodecs[R[_]] extends endpoints4s.algebra.JsonEntitiesFromCodecs {
  self: EndpointsWithCustomErrors[R] =>

  def jsonRequest[A](implicit codec: JsonCodec[A]): RequestEntity[A] = { (a, req) =>
    req.body(stringCodec(codec).encode(a)).contentType("application/json")
  }

  def jsonResponse[A](implicit codec: JsonCodec[A]): ResponseEntity[A] =
    stringCodecResponse(stringCodec(codec))

}
