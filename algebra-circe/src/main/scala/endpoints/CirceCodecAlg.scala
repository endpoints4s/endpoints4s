package endpoints

import io.circe.{Decoder, Encoder}

/**
  * Partial interpreter for [[JsonEntityAlg]]: only fixes the carrier types, but does
  * not implement the abstract methods.
  *
  * Uses [[CirceCodec]] for both HTTP responses and requests.
  *
  * It means that, from a client
  * point of view, you will need a complete code even if you only want to build a request
  * entity (despite that an [[Encoder]] would have sufficed).
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
  *   trait MyEndpoints extends EndpointAlg with CirceCodecAlg {
  *     val myEndpoint = endpoint(get(path), jsonResponse[MyDto])
  *   }
  * }}}
  *
  * {{{
  *   /* client MyEndpointsClient.scala */
  *
  *   object MyEndpointsClient extends MyEndpoints with CirceCodecXhrClient with XhrClientFaithful
  *
  *   MyEndpointsClient.myEndpoint().map(myDto => println(myDto.i))
  * }}}
  *
  * {{{
  *   /* server MyEndpointsServer.scala */
  *
  *   object MyEndpointsServer extends MyEndpoints with CirceCodecPlayRouting {
  *
  *     val routes = routesFromEndpoints(
  *       myEndpoint.implementedBy(_ => MyDto(42, "foo"))
  *     )
  *
  *   }
  * }}}
  */
trait CirceCodecAlg extends JsonEntityAlg {

  type JsonResponse[A] = CirceCodec[A]
  type JsonRequest[A] = CirceCodec[A]

}

/**
  * Combines both an [[Encoder]] and a [[Decoder]] into a single type class.
  *
  * You don’t need to define instances by yourself as they can be derived from an existing pair
  * of an [[Encoder]] and a [[Decoder]].
  *
  * @see https://github.com/travisbrown/circe/issues/301
  */
trait CirceCodec[A] {
  def encoder: Encoder[A]
  def decoder: Decoder[A]
}

object CirceCodec {

  @inline def apply[A](implicit codec: CirceCodec[A]): CirceCodec[A] = codec

  implicit def fromEncoderAndDecoder[A](implicit enc: Encoder[A], dec: Decoder[A]): CirceCodec[A] =
    new CirceCodec[A] {
      val decoder = dec
      val encoder = enc
    }

}