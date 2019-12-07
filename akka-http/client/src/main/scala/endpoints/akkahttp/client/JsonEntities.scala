package endpoints.akkahttp.client

import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import endpoints.algebra
import endpoints.algebra.Codec

/**
  * Interpreter for [[endpoints.algebra.JsonEntitiesFromCodecs]] that encodes JSON request
  * and decodes JSON responses using Akka HTTP.
  *
  * @group interpreters
  */
trait JsonEntitiesFromCodecs extends algebra.JsonEntitiesFromCodecs with EndpointsWithCustomErrors {

  def jsonRequest[A](implicit codec: JsonCodec[A]): RequestEntity[A] = { (a, req) =>
    req.copy(entity = HttpEntity(ContentTypes.`application/json`, stringCodec(codec).encode(a)))
  }

  def jsonResponse[A](implicit codec: JsonCodec[A]): ResponseEntity[A] = stringCodecResponse(stringCodec(codec))

}

/**
  * Interpreter for [[endpoints.algebra.JsonEntitiesFromSchemas]] that encodes JSON request
  * and decodes JSON responses using Akka HTTP.
  *
  * @group interpreters
  */
trait JsonEntitiesFromSchemas
  extends algebra.JsonEntitiesFromSchemas
    with JsonEntitiesFromCodecs
    with endpoints.ujson.JsonSchemas {

  def stringCodec[A](implicit codec: JsonCodec[A]): Codec[String, A] = codec.stringCodec

}
