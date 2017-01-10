package endpoints

import play.api.libs.ws.WSRequest

/**
  * [[MethodAlg]] interpreter that builds URLs.
  */
trait MethodClient extends MethodAlg {

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
