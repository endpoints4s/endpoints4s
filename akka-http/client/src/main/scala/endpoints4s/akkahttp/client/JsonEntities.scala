package endpoints4s.akkahttp.client

import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import endpoints4s.{Codec, algebra}

/** Interpreter for [[endpoints4s.algebra.JsonEntitiesFromCodecs]] that encodes JSON request
  * and decodes JSON responses using Akka HTTP.
  *
  * @group interpreters
  */
trait JsonEntitiesFromCodecs extends algebra.JsonEntitiesFromCodecs with EndpointsWithCustomErrors {

  def jsonRequest[A](implicit codec: JsonCodec[A]): RequestEntity[A] = { (a, req) =>
    req.withEntity(
      HttpEntity(
        ContentTypes.`application/json`,
        stringCodec(codec).encode(a)
      )
    )
  }

  def jsonResponse[A](implicit codec: JsonCodec[A]): ResponseEntity[A] =
    stringCodecResponse(stringCodec(codec))

}

/** Interpreter for [[endpoints4s.algebra.JsonEntitiesFromSchemas]] that encodes JSON request
  * and decodes JSON responses using Akka HTTP.
  *
  * @group interpreters
  */
trait JsonEntitiesFromSchemas
    extends algebra.JsonEntitiesFromSchemas
    with JsonEntitiesFromCodecs
    with endpoints4s.ujson.JsonSchemas {

  def stringCodec[A](implicit codec: JsonCodec[A]): Codec[String, A] =
    codec.stringCodec

}
