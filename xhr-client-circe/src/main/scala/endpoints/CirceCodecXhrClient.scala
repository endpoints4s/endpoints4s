package endpoints

import io.circe.parser
import org.scalajs.dom.raw.XMLHttpRequest

import scala.scalajs.js

/**
  * Implements [[CirceCodecAlg]] for [[EndpointXhrClient]].
  */
trait CirceCodecXhrClient extends EndpointXhrClient with CirceCodecAlg {

  /** Builds a request entity by using the supplied codec */
  def jsonRequest[A : CirceCodec] = (a: A, xhr: XMLHttpRequest) => {
    xhr.setRequestHeader("Content-Type", "application/json")
    CirceCodec[A].encoder.apply(a).noSpaces
  }

  /** Decodes the response entity by using the supplied codec */
  def jsonResponse[A : CirceCodec]: js.Function1[XMLHttpRequest, Either[Exception, A]] =
    xhr => parser.parse(xhr.responseText).flatMap(CirceCodec[A].decoder.decodeJson).toEither

}
