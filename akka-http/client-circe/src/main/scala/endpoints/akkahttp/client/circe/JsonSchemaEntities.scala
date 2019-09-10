package endpoints.akkahttp.client.circe

import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import endpoints._
import endpoints.akkahttp.client.Endpoints
import io.circe.{Encoder, Decoder}
import io.circe.syntax._
import io.circe.parser.decode

/**
  * Interpreter for [[endpoints.algebra.JsonSchemaEntities]] that uses circe’s [[io.circe.Encoder]] to encode
  * JSON entities in HTTP requests, and circe’s [[io.circe.Decoder]] to decode JSON entities
  * in HTTP responses.
  *
  * @group interpreters
  */
trait JsonSchemaEntities extends algebra.JsonSchemaEntities with circe.JsonSchemas { this: Endpoints =>

  def jsonRequest[A: JsonSchema]: RequestEntity[A] = { (a, req) =>
    implicit def encoder: Encoder[A] = implicitly[JsonSchema[A]].encoder
    req.copy(entity = HttpEntity(ContentTypes.`application/json`, a.asJson.spaces2))
  }

  def jsonResponse[A: JsonSchema]: ResponseEntity[A] = { entity =>
    implicit def decoder: Decoder[A] = implicitly[JsonSchema[A]].decoder
    for {
      strictEntity <- entity.toStrict(settings.toStrictTimeout)
    } yield decode[A](settings.stringContentExtractor(strictEntity))
  }
}
