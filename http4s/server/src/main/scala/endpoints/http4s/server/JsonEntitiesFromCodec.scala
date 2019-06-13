package endpoints.http4s.server

import cats.effect.Sync
import cats.implicits._
import endpoints.algebra.Documentation
import io.circe.Json
import io.circe.parser._
import org.http4s
import org.http4s.circe._
import org.http4s.{DecodeFailure, InvalidMessageBodyFailure}

abstract class JsonEntitiesFromCodec[F[_]](implicit F: Sync[F])
    extends BasicAuthentication
    with endpoints.algebra.circe.JsonEntitiesFromCodec {
  def jsonRequest[A](docs: Documentation = None)(
      implicit codec: JsonRequest[A]): RequestEntity[A] =
    req => {
      def transform(t: Either[DecodeFailure, Json]): Either[DecodeFailure, A] =
        for {
          str <- t
          a <- codec
            .decode(str.toString)
            .leftMap(error =>
              InvalidMessageBodyFailure(error.getMessage, Some(error)))
        } yield a

      val decoder: http4s.EntityDecoder[F, A] =
        jsonOf[F, Json].transform(transform)

      decoder.decode(req, true).value.flatMap(F.fromEither)
    }

  def jsonResponse[A](docs: Documentation = None)(
      implicit codec: JsonResponse[A]): Response[A] =
    a => {
      implicit val encoder: http4s.EntityEncoder[F, A] =
        jsonEncoderOf[F, Json].contramap(
          a =>
            // it should be safe call get here: A => Json => String => Either[Error, Json] => Json
            parse(codec.encode(a)).right.get)
      F.pure(http4s.Response().withEntity(a))
    }
}
