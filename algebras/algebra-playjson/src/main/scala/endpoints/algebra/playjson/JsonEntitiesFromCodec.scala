package endpoints.algebra.playjson

import endpoints.algebra.Codec
import play.api.libs.json.{Format, Json, JsResultException}

import scala.util.control.NonFatal

/**
  * Partial interpreter for [[endpoints.algebra.JsonEntitiesFromCodec]] that only
  * fixes the `JsonCodec[A]` type to Play’s `Format[A]`.
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
  *     import play.api.libs.{Format, Json}
  *
  *     implicit val jsonFormat: Format[MyDto] = Json.format[MyDto]
  *   }
  * }}}
  *
  * {{{
  *   /* shared endpoint definition */
  *
  *   trait MyEndpoints extends algebra.Endpoints with algebra.playjson.JsonEntitiesFromCodec {
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
  type JsonCodec[A] = Format[A]
//#type-carrier

  implicit def jsonCodec[A : Format]: Codec[String, A] = new Codec[String, A] {

    def decode(from: String): Either[Exception, A] =
      for {
        json <- (try { Right(Json.parse(from)) } catch { case NonFatal(e: Exception) => Left(e) }).right
        a <- Json.fromJson[A](json).asEither.left.map(JsResultException).right
      } yield a

    def encode(from: A): String = Json.stringify(Json.toJson(from))

  }

  // TODO Reads[A] to Decoder[Json, A] and Writes[A] to Encoder[A, Json]

}
