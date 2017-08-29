package endpoints.algebra

import endpoints.algebra.CirceEntities.CirceCodec
import io.circe.{Json, Decoder => CirceDecoder, Encoder => CirceEncoder}

/**
  * Partial interpreter for [[JsonEntities]]: only fixes the carrier types, but does
  * not implement the abstract methods.
  *
  * Uses [[CirceCodec]] for both HTTP responses and requests.
  *
  * It means that, from a client
  * point of view, you will need a complete code even if you only want to build a request
  * entity (despite that a [[io.circe.Encoder]] would have sufficed).
  *
  * The idea is to share codecs between client-side and server-side code,
  * thus guaranteeing consistency between both sides.
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
  *   trait MyEndpoints extends algebra.Endpoints with algebra.CirceEntities {
  *     val myEndpoint = endpoint(get(path), jsonResponse[MyDto])
  *   }
  * }}}
  *
  * {{{
  *   /* client MyEndpointsClient.scala */
  *
  *   object MyEndpointsClient extends MyEndpoints with xhr.CirceEntities with xhr.faithful.Endpoints
  *
  *   MyEndpointsClient.myEndpoint().map(myDto => println(myDto.i))
  * }}}
  *
  * {{{
  *   /* server MyEndpointsServer.scala */
  *
  *   object MyEndpointsServer extends MyEndpoints with play.server.CirceEntities {
  *
  *     val routes = routesFromEndpoints(
  *       myEndpoint.implementedBy(_ => MyDto(42, "foo"))
  *     )
  *
  *   }
  * }}}
  */
trait CirceEntities extends JsonEntities {

  type JsonResponse[A] = CirceCodec[A]
  type JsonRequest[A] = CirceCodec[A]

  /** Provides a Json [[Decoder]] based on an existing circe decoder */
  implicit def circeJsonDecoder[A](implicit circeDecoder: CirceDecoder[A]): Decoder[Json, A] =
    new Decoder[Json, A] {
      def decode(from: Json): Either[Throwable, A] = circeDecoder
        .decodeJson(from)
        .fold(failure => Left(new Exception(failure.message)), Right(_))
    }


  /** Provides a Json [[Encoder]] based on an existing circe encoder */
  implicit def circeJsonEncoder[A](implicit circeEncoder: CirceEncoder[A]): Encoder[A, Json] =
    new Encoder[A, Json] {
      def encode(from: A): Json = circeEncoder(from)
    }

}

object CirceEntities {

  /**
    * Combines both an [[io.circe.Encoder]] and a [[io.circe.Decoder]] into a single type class.
    *
    * You don’t need to define instances by yourself as they can be derived from an existing pair
    * of an [[io.circe.Encoder]] and a [[io.circe.Decoder]].
    *
    * @see https://github.com/travisbrown/circe/issues/301
    */
  trait CirceCodec[A] {
    def encoder: CirceEncoder[A]

    def decoder: CirceDecoder[A]
  }

  object CirceCodec {

    @inline def apply[A](implicit codec: CirceCodec[A]): CirceCodec[A] = codec

    implicit def fromEncoderAndDecoder[A](implicit enc: CirceEncoder[A], dec: CirceDecoder[A]): CirceCodec[A] =
      new CirceCodec[A] {
        val decoder = dec
        val encoder = enc
      }

  }

}
