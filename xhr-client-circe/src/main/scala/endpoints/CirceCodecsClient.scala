package endpoints

import io.circe.parser
import org.scalajs.dom.raw.XMLHttpRequest

import scala.scalajs.js

trait CirceCodecsClient extends XhrClient with CirceCodecs {

  def jsonRequest[A : CirceCodec] = (a: A, xhr: XMLHttpRequest) => {
    xhr.setRequestHeader("Content-Type", "application/json")
    CirceCodec[A].encoder.apply(a).noSpaces
  }

  def jsonResponse[A : CirceCodec]: js.Function1[XMLHttpRequest, Either[Exception, A]] =
    xhr => parser.parse(xhr.responseText).flatMap(CirceCodec[A].decoder.decodeJson).toEither

}
