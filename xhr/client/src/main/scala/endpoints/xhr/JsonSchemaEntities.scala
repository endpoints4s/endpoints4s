package endpoints.xhr

import endpoints.algebra
import org.scalajs.dom.XMLHttpRequest

trait JsonSchemaEntities
  extends algebra.JsonSchemaEntities
    with EndpointsWithCustomErrors
    with endpoints.ujson.JsonSchemas {

  def jsonRequest[A](implicit codec: JsonRequest[A]) = (a: A, xhr: XMLHttpRequest) => {
    xhr.setRequestHeader("Content-Type", "application/json")
    codec.stringCodec.encode(a)
  }

  def jsonResponse[A](implicit codec: JsonResponse[A]) =
    stringCodecResponse(codec.stringCodec)

}
