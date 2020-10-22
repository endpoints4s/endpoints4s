package endpoints4s.xhr.circe

import endpoints4s.algebra
import endpoints4s.xhr.EndpointsWithCustomErrors
import io.circe.{parser, Decoder => CirceDecoder, Encoder => CirceEncoder}
import org.scalajs.dom.XMLHttpRequest

/** An interpreter for [[algebra.JsonEntities]] that uses circe’s [[io.circe.Encoder]] to build JSON
  * entities in HTTP requests, and circe’s [[io.circe.Decoder]] to decode JSON entities from
  * HTTP responses.
  *
  * @group interpreters
  */
trait JsonEntities extends EndpointsWithCustomErrors with algebra.JsonEntities {

  /** Encode an `A` using circe’s [[io.circe.Encoder]] */
  type JsonRequest[A] = CirceEncoder[A]

  /** Decodes an `A` using circe’s [[io.circe.Decoder]] */
  type JsonResponse[A] = CirceDecoder[A]

  def jsonRequest[A: JsonRequest]: RequestEntity[A] =
    (a: A, xhr: XMLHttpRequest) => {
      xhr.setRequestHeader("Content-Type", "application/json")
      CirceEncoder[A].apply(a).noSpaces
    }

  def jsonResponse[A](implicit decoder: CirceDecoder[A]): ResponseEntity[A] =
    xhr => parser.parse(xhr.responseText).flatMap(decoder.decodeJson)

}
