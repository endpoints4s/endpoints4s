package endpoints.akkahttp.server.circe

import akka.http.scaladsl.server.{Directives, MalformedRequestContentRejection, Rejection}
import cats.Show
import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport._
import endpoints._
import endpoints.akkahttp.server
import io.circe.{Decoder, DecodingFailure, Encoder}

/**
  * Interpreter for [[algebra.JsonEntities]] that uses circe’s [[io.circe.Decoder]] to decode
  * JSON entities in HTTP requests, and circe’s [[io.circe.Encoder]] to build JSON entities
  * in HTTP responses.
  *
  * @group interpreters
  */
trait JsonSchemaEntities extends server.EndpointsWithCustomErrors with algebra.JsonSchemaEntities with circe.JsonSchemas {

  def jsonRequest[A : JsonSchema]: RequestEntity[A] = {
    implicit def decoder: Decoder[A] = implicitly[JsonSchema[A]].decoder
    Directives.entity[A](implicitly)
      .recoverPF(Function.unlift { rejections: Seq[Rejection] =>
        val decodingErrors =
          rejections.collect {
            case MalformedRequestContentRejection(_, DecodingFailures(errors)) =>
              errors.map(Show[DecodingFailure].show).toList
          }.flatten
        if (decodingErrors.isEmpty) None
        else Some(handleClientErrors(Invalid(decodingErrors)))
      })
  }

  def jsonResponse[A : JsonSchema]: ResponseEntity[A] = {
    implicit def encoder: Encoder[A] = implicitly[JsonSchema[A]].encoder
    implicitly
  }

}
