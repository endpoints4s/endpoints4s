package endpoints.xhr.circe

import endpoints.algebra
import endpoints.xhr.Endpoints
import io.circe.{Decoder, Encoder, parser}
import org.scalajs.dom.raw.XMLHttpRequest

import scala.scalajs.js

/**
  * An interpreter for [[algebra.JsonEntities]] that uses circe’s [[Encoder]] to build JSON
  * entities in HTTP requests, and circe’s [[Decoder]] to decode JSON entities from
  * HTTP responses.
  */
trait JsonEntities extends Endpoints with algebra.JsonEntities {

  /** Encode an `A` using circe’s [[Encoder]] */
  type JsonRequest[A] = Encoder[A]

  /** Decodes an `A` using circe’s [[Decoder]] */
  type JsonResponse[A] = Decoder[A]

  def jsonRequest[A : JsonRequest] = (a: A, xhr: XMLHttpRequest) => {
    xhr.setRequestHeader("Content-Type", "application/json")
    Encoder[A].apply(a).noSpaces
  }

  def jsonResponse[A](implicit decoder: Decoder[A]): js.Function1[XMLHttpRequest, Either[Exception, A]] =
    xhr => parser.parse(xhr.responseText).right.flatMap(decoder.decodeJson)

}
