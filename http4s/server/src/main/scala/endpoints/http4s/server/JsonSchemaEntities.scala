package endpoints.http4s.server

import cats.implicits._
import cats.effect.Sync
import endpoints.algebra
import endpoints.algebra.Documentation
import io.circe.{Decoder, Encoder}
import org.http4s
import org.http4s.circe._

abstract class JsonSchemaEntities[F[_]](implicit F: Sync[F])
    extends Endpoints
    with algebra.JsonSchemaEntities
    with endpoints.circe.JsonSchemas {

  def jsonRequest[A: JsonSchema](docs: Documentation): RequestEntity[A] = {
    entity =>
      implicit def decoder: Decoder[A] = implicitly[JsonSchema[A]].decoder
      implicit val jsonDecoder: http4s.EntityDecoder[F, A] = jsonOf[F, A]

      jsonDecoder.decode(entity, true).value.flatMap(F.fromEither)
  }

  def jsonResponse[A: JsonSchema](
      docs: Documentation): A => F[http4s.Response[F]] = { a =>
    implicit def encoder: Encoder[A] = implicitly[JsonSchema[A]].encoder
    implicit val jsonEncoder: http4s.EntityEncoder[F, A] = jsonEncoderOf[F, A]

    F.pure(http4s.Response().withEntity(a))
  }

}
