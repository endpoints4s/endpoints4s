package endpoints.xhr.circe

import endpoints.algebra
import endpoints.algebra.Documentation
import endpoints.xhr.Endpoints
import io.circe.{parser, Decoder => CirceDecoder, Encoder => CirceEncoder}
import org.scalajs.dom.raw.XMLHttpRequest

/**
  * An interpreter for [[algebra.JsonEntities]] that uses circe’s [[io.circe.Encoder]] to build JSON
  * entities in HTTP requests, and circe’s [[io.circe.Decoder]] to decode JSON entities from
  * HTTP responses.
  */
trait JsonEntities extends Endpoints with algebra.JsonEntities {

  /** Encode an `A` using circe’s [[io.circe.Encoder]] */
  type JsonRequest[A] = CirceEncoder[A]

  /** Decodes an `A` using circe’s [[io.circe.Decoder]] */
  type JsonResponse[A] = CirceDecoder[A]

  def jsonRequest[A : JsonRequest](docs: Documentation): RequestEntity[A] = (a: A, xhr: XMLHttpRequest) => {
    xhr.setRequestHeader("Content-Type", "application/json")
    CirceEncoder[A].apply(a).noSpaces
  }

  def jsonResponse[A](docs: Documentation)(implicit decoder: CirceDecoder[A]): Response[A] =
    xhr => parser.parse(xhr.responseText).right.flatMap(decoder.decodeJson)

}
