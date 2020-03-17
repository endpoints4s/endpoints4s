package endpoints.http4s.client

import cats.implicits._
import endpoints.algebra.Codec

trait JsonEntitiesFromCodecs
    extends EndpointsWithCustomErrors
    with endpoints.algebra.JsonEntitiesFromCodecs {

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
    extends endpoints.algebra.JsonEntitiesFromSchemas
    with JsonEntitiesFromCodecs
    with endpoints.ujson.JsonSchemas {

  def stringCodec[A](implicit codec: JsonCodec[A]): Codec[String, A] =
    codec.stringCodec

}
