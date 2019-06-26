package endpoints.http4s.server.circe

import cats.implicits._
import endpoints.algebra
import endpoints.algebra.Documentation
import endpoints.http4s.server.Endpoints
import io.circe.{Decoder, Encoder}
import org.http4s
import org.http4s.circe._

trait JsonSchemaEntities
    extends Endpoints
    with algebra.JsonSchemaEntities
    with endpoints.circe.JsonSchemas {

  def jsonRequest[A: JsonSchema](docs: Documentation): RequestEntity[A] = {
    entity =>
      implicit val decoder: Decoder[A] = implicitly[JsonSchema[A]].decoder
      implicit val jsonDecoder: http4s.EntityDecoder[Effect, A] = jsonOf[Effect, A]

      jsonDecoder.decode(entity, true).value.flatMap(Effect.fromEither)
  }

  def jsonResponse[A: JsonSchema](
      docs: Documentation): A => http4s.Response[Effect] = { a =>
    implicit val encoder: Encoder[A] = implicitly[JsonSchema[A]].encoder
    implicit val jsonEncoder: http4s.EntityEncoder[Effect, A] = jsonEncoderOf[Effect, A]

    http4s.Response[Effect]().withEntity(a)
  }

}
