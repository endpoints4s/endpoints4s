package endpoints
import org.scalajs.dom.XMLHttpRequest

import scala.scalajs.js

trait OptionalResponseClient extends OptionalResponseAlg with XhrClient {

  /**
    * A response decoder that maps HTTP responses having status code 404 to `None`, or delegates to the given `response`.
    */
  def option[A](
    response: js.Function1[XMLHttpRequest, Either[Exception, A]]
  ): js.Function1[XMLHttpRequest, Either[Exception, Option[A]]] =
    xhr =>
      if (xhr.status == 404) Right(None)
      else response(xhr).right.map(Some(_))

}
