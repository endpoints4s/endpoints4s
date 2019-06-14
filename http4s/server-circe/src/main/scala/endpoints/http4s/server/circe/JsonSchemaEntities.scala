package endpoints.http4s.server.circe

import cats.implicits._
import endpoints.algebra
import endpoints.algebra.Documentation
import endpoints.http4s.server.Endpoints
import io.circe.{Decoder, Encoder}
import org.http4s
import org.http4s.circe._

trait JsonSchemaEntities[F[_]]
    extends Endpoints[F]
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

    http4s.Response[F]().withEntity(a).pure[F]
  }

}
