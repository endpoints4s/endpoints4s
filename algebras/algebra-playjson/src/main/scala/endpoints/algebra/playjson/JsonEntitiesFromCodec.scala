package endpoints.algebra.playjson

import endpoints.{Invalid, Valid, Validated}
import endpoints.algebra.Codec
import play.api.libs.json.{Format, JsPath, Json, JsonValidationError}

import scala.util.{Failure, Success, Try}

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

    def decode(from: String): Validated[A] =
      (Try(Json.parse(from)) match {
        case Failure(_) => Left(Invalid("Unable to parse entity as JSON"))
        case Success(a) => Right(a)
      }).right.flatMap { json =>
          def showErrors(errors: collection.Seq[(JsPath, collection.Seq[JsonValidationError])]): Invalid =
            Invalid((
              for {
                (path, pathErrors) <- errors.iterator
                error <- pathErrors
              } yield s"${error.message} for ${path.toJsonString}"
            ).toSeq)
          Json.fromJson[A](json).asEither
            .left.map(showErrors)
            .right.map(Valid(_))
        }.merge

    def encode(from: A): String = Json.stringify(Json.toJson(from))

  }

}
