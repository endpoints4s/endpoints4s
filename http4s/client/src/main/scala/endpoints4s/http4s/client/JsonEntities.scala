package endpoints4s.http4s.client

import cats.implicits._
import endpoints4s.algebra.Codec

trait JsonEntitiesFromCodecs
    extends EndpointsWithCustomErrors
    with endpoints4s.algebra.JsonEntitiesFromCodecs {

  def jsonRequest[A](implicit codec: JsonCodec[A]): RequestEntity[A] =
    (a, request) => request.withEntity(stringCodec(codec).encode(a))

  def jsonResponse[A](implicit codec: JsonCodec[A]): ResponseEntity[A] =
    _.as[String].flatMap(body =>
      stringCodec(codec)
        .decode(body)
        .fold(
          effect.pure,
          errors => effect.raiseError(new Exception(errors.mkString(", ")))
        )
    )

}

trait JsonEntitiesFromSchemas
    extends endpoints4s.algebra.JsonEntitiesFromSchemas
    with JsonEntitiesFromCodecs
    with endpoints4s.ujson.JsonSchemas {

  def stringCodec[A](implicit codec: JsonCodec[A]): Codec[String, A] =
    codec.stringCodec

}
