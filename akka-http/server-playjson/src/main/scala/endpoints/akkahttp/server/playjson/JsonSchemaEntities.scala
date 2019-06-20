package endpoints.akkahttp.server.playjson

import akka.http.scaladsl.marshalling.{Marshaller, ToResponseMarshaller}
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.unmarshalling.Unmarshaller
import endpoints.akkahttp.server
import endpoints.algebra.Documentation
import endpoints.algebra
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import endpoints._

import akka.http.scaladsl.marshalling.{Marshaller, ToResponseMarshaller}
import akka.http.scaladsl.unmarshalling.Unmarshaller
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import endpoints.algebra.Documentation

/**
  * Interpreter for [[algebra.JsonEntities]] that uses Play JSON [[play.api.libs.json.Reads]] to decode
  * JSON entities in HTTP requests, and [[play.api.libs.json.Writes]] to build JSON entities
  * in HTTP responses.
  *
  * @group interpreters
  */
trait JsonSchemaEntities extends server.Endpoints with algebra.JsonSchemaEntities with endpoints.playjson.JsonSchemas {

  def jsonRequest[A: JsonSchema](docs: Documentation): RequestEntity[A] = {
    Directives.entity[A](
      Unmarshaller.messageUnmarshallerFromEntityUnmarshaller(
        PlayJsonSupport.unmarshaller(implicitly[JsonSchema[A]].reads)))
  }

  def jsonResponse[A: JsonSchema](docs: Documentation): Response[A] = { a =>
    implicit val m: ToResponseMarshaller[A] =
      Marshaller.liftMarshaller(PlayJsonSupport.marshaller(implicitly[JsonSchema[A]].writes))

    Directives.complete(a)
  }
}
