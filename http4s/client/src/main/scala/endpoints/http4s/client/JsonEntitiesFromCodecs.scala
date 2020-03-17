package endpoints.http4s.client

import cats.implicits._

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
