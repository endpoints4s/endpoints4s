package endpoints.xhr

import endpoints.algebra.Documentation
import org.scalajs.dom.XMLHttpRequest

/**
  * Interpreter for [[endpoints.algebra.JsonEntitiesFromCodec]] that encodes JSON requests
  * and decodes JSON responses.
  *
  * @group interpreters
  */
trait JsonEntitiesFromCodec extends Endpoints with endpoints.algebra.JsonEntitiesFromCodec {

  def jsonRequest[A](docs: Documentation)(implicit codec: JsonCodec[A]) = (a: A, xhr: XMLHttpRequest) => {
    xhr.setRequestHeader("Content-Type", "application/json")
    jsonCodecToCodec(codec).encode(a)
  }

  def jsonResponse[A](docs: Documentation)(implicit codec: JsonCodec[A]) =
    (xhr: XMLHttpRequest) => jsonCodecToCodec(codec).decode(xhr.responseText)

}
