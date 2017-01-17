package endpoints.xhr

import io.circe.parser
import org.scalajs.dom.raw.XMLHttpRequest
import endpoints.algebra
import endpoints.algebra.CirceEntities.CirceCodec

import scala.scalajs.js

/**
  * Implements [[algebra.CirceEntities]] for [[Endpoints]].
  */
trait CirceEntities extends Endpoints with algebra.CirceEntities {

  /** Builds a request entity by using the supplied codec */
  def jsonRequest[A : CirceCodec] = (a: A, xhr: XMLHttpRequest) => {
    xhr.setRequestHeader("Content-Type", "application/json")
    CirceCodec[A].encoder.apply(a).noSpaces
  }

  /** Decodes the response entity by using the supplied codec */
  def jsonResponse[A : CirceCodec]: js.Function1[XMLHttpRequest, Either[Exception, A]] =
    xhr => parser.parse(xhr.responseText).right.flatMap(CirceCodec[A].decoder.decodeJson _)

}
