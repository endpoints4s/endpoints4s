package endpoints

import play.api.libs.ws.WSRequest

/**
  * [[MethodsAlg]] interpreter that builds URLs.
  */
trait MethodsClient extends MethodsAlg {

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
