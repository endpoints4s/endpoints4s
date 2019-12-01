package endpoints.xhr

import endpoints.algebra
import org.scalajs.dom.XMLHttpRequest

/**
  * Interpreter for [[endpoints.algebra.JsonEntitiesFromCodec]] that encodes JSON requests
  * and decodes JSON responses.
  *
  * @group interpreters
  */
trait JsonEntitiesFromCodec extends EndpointsWithCustomErrors with algebra.JsonEntitiesFromCodec {

  def jsonRequest[A](implicit codec: JsonCodec[A]) = (a: A, xhr: XMLHttpRequest) => {
    xhr.setRequestHeader("Content-Type", "application/json")
    stringCodec(codec).encode(a)
  }

  def jsonResponse[A](implicit codec: JsonCodec[A]) = stringCodecResponse(stringCodec(codec))

}
