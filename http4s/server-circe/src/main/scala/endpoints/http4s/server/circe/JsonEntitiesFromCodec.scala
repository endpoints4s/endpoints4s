package endpoints.http4s.server.circe

import cats.implicits._
import endpoints.algebra.Documentation
import endpoints.http4s.server.Endpoints
import fs2.Chunk
import org.http4s
import org.http4s.headers.`Content-Type`
import org.http4s.{
  DecodeResult,
  EntityEncoder,
  InvalidMessageBodyFailure,
  MediaType
}

trait JsonEntitiesFromCodec
    extends Endpoints
    with endpoints.algebra.circe.JsonEntitiesFromCodec {

  def jsonRequest[A](docs: Documentation = None)(
      implicit codec: JsonRequest[A]): RequestEntity[A] =
    req => {
      val decoder = http4s.EntityDecoder
        .decodeBy[Effect, String](MediaType.application.json)(msg =>
          DecodeResult.success(msg.bodyAsText.compile.foldMonoid))
        .transform(
          _.flatMap { value =>
            codec
              .decode(value)
              .leftMap(error =>
                InvalidMessageBodyFailure(error.getMessage, Some(error)))
          }
        )

      decoder.decode(req, true).value.flatMap(Effect.fromEither)
    }

  def jsonResponse[A](docs: Documentation = None)(
      implicit codec: JsonResponse[A]): Response[A] =
    a => {
      implicit val encoder: http4s.EntityEncoder[Effect, A] =
        EntityEncoder[Effect, Chunk[Byte]]
          .contramap[A](value => Chunk.bytes(codec.encode(value).getBytes()))
          .withContentType(`Content-Type`(MediaType.application.json))

      http4s.Response[Effect]().withEntity(a)
    }
}
