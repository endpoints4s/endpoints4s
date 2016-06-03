package endpoints

import cats.data.Xor
import io.circe.{Decoder, Encoder, parser}
import org.scalajs.dom.raw.XMLHttpRequest

import scala.scalajs.js

trait CirceClient extends XhrClient with JsonEntitiesAlg {

  type JsonRequest[A] = Encoder[A]
  type JsonResponse[A] = Decoder[A]

  def jsonRequest[A : JsonRequest] = (a: A, xhr: XMLHttpRequest) => {
    xhr.setRequestHeader("Content-Type", "application/json")
    Encoder[A].apply(a).noSpaces
  }

  def jsonResponse[A](implicit decoder: Decoder[A]): js.Function1[XMLHttpRequest, Xor[Exception, A]] =
    xhr => parser.parse(xhr.responseText).flatMap(decoder.decodeJson)

}
