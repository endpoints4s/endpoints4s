package endpoints.akkahttp.server

import akka.http.scaladsl.server.Directives
import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport._
import endpoints._
import endpoints.akkahttp.server
import endpoints.algebra.Documentation
import io.circe.{Decoder, Encoder}

/**
  * Interpreter for [[algebra.JsonEntities]] that uses circe’s [[io.circe.Decoder]] to decode
  * JSON entities in HTTP requests, and circe’s [[io.circe.Encoder]] to build JSON entities
  * in HTTP responses.
  *
  * @group interpreters
  */
trait JsonSchemaEntities extends server.Endpoints with algebra.JsonSchemaEntities with circe.JsonSchemas {

  def jsonRequest[A : JsonSchema](docs: Documentation): RequestEntity[A] = {
    implicit def decoder: Decoder[A] = implicitly[JsonSchema[A]].decoder
    Directives.entity[A](implicitly)
  }

  def jsonResponse[A : JsonSchema](docs: Documentation): Response[A] = { a =>
    implicit def encoder: Encoder[A] = implicitly[JsonSchema[A]].encoder
    Directives.complete(a)
  }
}
