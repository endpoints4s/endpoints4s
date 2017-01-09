package endpoints

import play.api.mvc.RequestHeader

/**
  * [[MethodsAlg]] interpreter that decodes and encodes methods.
  */
trait MethodsPlayRouting extends MethodsAlg {

  case class Method(value: String) {
    val extract = (request: RequestHeader) =>
      if (request.method == value) Some(())
      else None
  }

  override def Get = Method("GET")

  override def Post = Method("POST")

  override def Put = Method("PUT")

  override def Delete = Method("DELETE")
}

