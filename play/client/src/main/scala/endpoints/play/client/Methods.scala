package endpoints.play.client

import endpoints.algebra
import play.api.libs.ws.WSRequest

/**
  * [[algebra.Methods]] interpreter that builds URLs.
  */
trait Methods extends algebra.Methods {

  /**
    * String representation of the method
    */
  type Method = WSRequest => WSRequest

  private def setMethod(method: String): Method = (r: WSRequest) => r.withMethod(method)

  def Get = setMethod("GET")

  def Post = setMethod("POST")

  def Put = setMethod("PUT")

  def Delete = setMethod("DELETE")

}
