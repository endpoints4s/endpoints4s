package endpoints.sttp.client

import endpoints.algebra.Codec

/**
  * Interpreter for [[endpoints.algebra.JsonEntitiesFromCodec]] that encodes JSON request
  * @group interpreters
  */
trait JsonEntitiesFromCodec[R[_]] extends endpoints.algebra.JsonEntitiesFromCodec { self: EndpointsWithCustomErrors[R] =>

  def jsonRequest[A](implicit codec: Codec[String, A]): RequestEntity[A] = { (a, req) =>
    req.body(codec.encode(a)).contentType("application/json")
  }

  def jsonResponse[A](implicit codec: Codec[String, A]): ResponseEntity[A] = stringCodecResponse

}
