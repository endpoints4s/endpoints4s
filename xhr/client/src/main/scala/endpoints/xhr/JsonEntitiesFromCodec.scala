package endpoints.xhr

import endpoints.algebra.Codec
import org.scalajs.dom.XMLHttpRequest

/**
  * Interpreter for [[endpoints.algebra.JsonEntitiesFromCodec]] that encodes JSON requests
  * and decodes JSON responses.
  *
  * @group interpreters
  */
trait JsonEntitiesFromCodec extends Endpoints with endpoints.algebra.JsonEntitiesFromCodec {

  def jsonRequest[A](implicit codec: Codec[String, A]) = (a: A, xhr: XMLHttpRequest) => {
    xhr.setRequestHeader("Content-Type", "application/json")
    codec.encode(a)
  }

  def jsonResponse[A](implicit codec: Codec[String, A]) =
    (xhr: XMLHttpRequest) =>
      codec.decode(xhr.responseText).fold(Right(_), errors => Left(new Exception(errors.mkString(". "))))

}
