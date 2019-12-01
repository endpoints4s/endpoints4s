package endpoints.akkahttp.client

import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import endpoints.algebra

/**
  * Interpreter for [[endpoints.algebra.JsonEntitiesFromCodec]] that encodes JSON request
  * and decodes JSON responses using Akka HTTP.
  *
  * @group interpreters
  */
trait JsonEntitiesFromCodec extends algebra.JsonEntitiesFromCodec with EndpointsWithCustomErrors {

  def jsonRequest[A](implicit codec: JsonCodec[A]): RequestEntity[A] = { (a, req) =>
    req.copy(entity = HttpEntity(ContentTypes.`application/json`, stringCodec(codec).encode(a)))
  }

  def jsonResponse[A](implicit codec: JsonCodec[A]): ResponseEntity[A] = stringCodecResponse(stringCodec(codec))

}
