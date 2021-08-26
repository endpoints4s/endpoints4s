package endpoints4s.play.server

import endpoints4s.algebra
import play.api.mvc.RequestHeader

/** [[algebra.Methods]] interpreter that decodes and encodes methods.
  *
  * @group interpreters
  */
trait Methods extends algebra.Methods {

  case class Method(value: String) {
    def matches(requestHeader: RequestHeader): Boolean =
      requestHeader.method == value
  }

  def Get = Method("GET")

  def Post = Method("POST")

  def Put = Method("PUT")

  def Delete = Method("DELETE")

  def Options = Method("OPTIONS")

  def Patch = Method("PATCH")
}
