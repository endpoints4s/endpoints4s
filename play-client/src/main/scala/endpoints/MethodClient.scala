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

  private def setMethod(method:String): Method = (r: WSRequest) => r.withMethod(method)

  override def Get = setMethod("GET")
  override def Post = setMethod("POST")
  override def Put = setMethod("PUT")
  override def Delete = setMethod("DELETE")

}
