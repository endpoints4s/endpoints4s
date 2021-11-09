package endpoints4s.fetch.circe

import endpoints4s.algebra
import endpoints4s.fetch.EndpointsWithCustomErrors
import io.circe.parser
import io.circe.{Decoder => CirceDecoder}
import io.circe.{Encoder => CirceEncoder}
import org.scalajs.dom.experimental.{RequestInit => FetchRequestInit}

import scala.scalajs.js.Thenable.Implicits._

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
    (a: A, requestInit: FetchRequestInit) => {
      requestInit.setRequestHeader("Content-Type", "application/json")
      requestInit.body = CirceEncoder[A].apply(a).noSpaces
    }

  def jsonResponse[A](implicit decoder: CirceDecoder[A]): ResponseEntity[A] =
    response => response.text().map(text => parser.parse(text).flatMap(decoder.decodeJson))

}
