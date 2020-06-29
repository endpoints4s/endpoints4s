package endpoints4s.play.client

import endpoints4s.algebra
import play.api.libs.ws.WSRequest

/**
  * [[algebra.Methods]] interpreter that builds URLs.
  *
  * @group interpreters
  */
trait Methods extends algebra.Methods {

  /**
    * String representation of the method
    */
  type Method = WSRequest => WSRequest

  private def setMethod(method: String): Method =
    (r: WSRequest) => r.withMethod(method)

  def Get = setMethod("GET")

  def Post = setMethod("POST")

  def Put = setMethod("PUT")

  def Delete = setMethod("DELETE")

  def Options = setMethod("OPTIONS")

  def Patch = setMethod("PATCH")

}
