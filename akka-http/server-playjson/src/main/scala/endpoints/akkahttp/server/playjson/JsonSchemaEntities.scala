package endpoints.akkahttp.server.playjson

import akka.http.scaladsl.server.{Directives, Rejection, ValidationRejection}
import akka.http.scaladsl.unmarshalling.Unmarshaller
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport.PlayJsonError
import endpoints.akkahttp.server
import endpoints.{Invalid, algebra}

/**
  * Interpreter for [[algebra.JsonEntities]] that uses Play JSON `play.api.libs.json.Reads` to decode
  * JSON entities in HTTP requests, and `play.api.libs.json.Writes` to build JSON entities
  * in HTTP responses.
  *
  * @group interpreters
  */
trait JsonSchemaEntities extends server.EndpointsWithCustomErrors with algebra.JsonSchemaEntities with endpoints.playjson.JsonSchemas {

  def jsonRequest[A: JsonSchema]: RequestEntity[A] = {
    Directives.entity[A](
      Unmarshaller.messageUnmarshallerFromEntityUnmarshaller(
        PlayJsonSupport.unmarshaller(implicitly[JsonSchema[A]].reads))
    ).recoverPF(Function.unlift { rejections: Seq[Rejection] =>
      val decodingErrors =
        rejections.collect {
          case ValidationRejection(_, Some(PlayJsonError(error))) =>
            for {
              (path, pathErrors) <- error.errors.iterator
              error <- pathErrors
            } yield s"${error.message} for ${path.toJsonString}"
        }.flatten
      if (decodingErrors.isEmpty) None
      else Some(handleClientErrors(Invalid(decodingErrors)))
    })
  }

  def jsonResponse[A: JsonSchema]: ResponseEntity[A] =
    PlayJsonSupport.marshaller(implicitly[JsonSchema[A]].writes)

}
