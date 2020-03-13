package endpoints.xhr

import endpoints.algebra
import endpoints.algebra.Codec
import org.scalajs.dom.XMLHttpRequest

/**
  * Interpreter for [[endpoints.algebra.JsonEntitiesFromCodecs]] that encodes JSON requests
  * and decodes JSON responses.
  *
  * @group interpreters
  */
trait JsonEntitiesFromCodecs
    extends EndpointsWithCustomErrors
    with algebra.JsonEntitiesFromCodecs {

  def jsonRequest[A](implicit codec: JsonCodec[A]) =
    (a: A, xhr: XMLHttpRequest) => {
      xhr.setRequestHeader("Content-Type", "application/json")
      stringCodec(codec).encode(a)
    }

  def jsonResponse[A](implicit codec: JsonCodec[A]) =
    stringCodecResponse(stringCodec(codec))

}

/**
  * Interpreter for [[endpoints.algebra.JsonEntitiesFromCodecs]] that encodes JSON requests
  * and decodes JSON responses.
  *
  * @group interpreters
  */
trait JsonEntitiesFromSchemas
    extends algebra.JsonEntitiesFromSchemas
    with JsonEntitiesFromCodecs
    with endpoints.ujson.JsonSchemas {

  def stringCodec[A](implicit codec: JsonCodec[A]): Codec[String, A] =
    codec.stringCodec

}
