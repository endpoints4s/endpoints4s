package endpoints.algebra.circe

import cats.Show
import endpoints.{Invalid, Valid, Validated}
import endpoints.algebra.{Codec, Decoder, Encoder}
import io.circe.{DecodingFailure, Json, ParsingFailure, parser, Decoder => CirceDecoder, Encoder => CirceEncoder}

/**
  * Partial interpreter for [[endpoints.algebra.JsonEntitiesFromCodec]] that only
  * fixes the `JsonCodec[A]` type to a [[CirceCodec]].
  *
  * The `jsonRequest` and `jsonResponse` operations have to be implemented by
  * a more specialized interpreter.
  *
  * Typical usage:
  *
  * {{{
  *   /* shared MyDto.scala */
  *
  *   case class MyDto(i: Int, s: String)
  *
  *   object MyDto {
  *     import io.circe.{Encoder, Decoder}
  *     import io.circe.generic.semiauto.{deriveEncoder, deriveDecoder}
  *
  *     implicit val encoder: Encoder[MyDto] = deriveEncoder
  *     implicit val decoder: Decoder[MyDto] = deriveDecoder
  *   }
  * }}}
  *
  * {{{
  *   /* shared endpoint definition */
  *
  *   trait MyEndpoints extends algebra.Endpoints with algebra.circe.JsonEntitiesFromCodec {
  *     val myEndpoint = endpoint(get(path), jsonResponse[MyDto])
  *   }
  * }}}
  *
  * {{{
  *   /* client MyEndpointsClient.scala */
  *
  *   object MyEndpointsClient extends MyEndpoints with xhr.JsonEntitiesFromCodec with xhr.faithful.Endpoints
  *
  *   MyEndpointsClient.myEndpoint().map(myDto => println(myDto.i))
  * }}}
  *
  * {{{
  *   /* server MyEndpointsServer.scala */
  *
  *   object MyEndpointsServer extends MyEndpoints with play.server.JsonEntitiesFromCodec {
  *
  *     val routes = routesFromEndpoints(
  *       myEndpoint.implementedBy(_ => MyDto(42, "foo"))
  *     )
  *
  *   }
  * }}}
  *
  */
trait JsonEntitiesFromCodec extends endpoints.algebra.JsonEntitiesFromCodec {

//#type-carrier
  type JsonCodec[A] = CirceCodec[A]
//#type-carrier

  implicit def jsonCodec[A](implicit codec: CirceCodec[A]): Codec[String, A] = new Codec[String, A] {

    def decode(from: String): Validated[A] =
      parser.parse(from).left.map(Show[ParsingFailure].show)
        .right.flatMap { json =>
          codec.decoder.decodeJson(json).left.map(Show[DecodingFailure].show)
        }
        .fold(Invalid(_), Valid(_))

    def encode(from: A): String =
      codec.encoder.apply(from).noSpaces

  }

  implicit def circeDecoderToDecoder[A](implicit decoder: CirceDecoder[A]): Decoder[Json, A] =
    new Decoder[Json, A] {
      def decode(from: Json): Validated[A] =
        decoder.decodeJson(from)
          .fold(failure => Invalid(Show[DecodingFailure].show(failure)), Valid(_))
    }

  implicit def circeEncoderToEncoder[A](implicit encoder: CirceEncoder[A]): Encoder[A, Json] =
    new Encoder[A, Json] {
      def encode(from: A): Json = encoder(from)
    }

}
