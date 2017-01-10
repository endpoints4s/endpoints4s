package endpoints

import play.api.mvc.RequestHeader

/**
  * [[MethodAlg]] interpreter that decodes and encodes methods.
  */
trait MethodPlayRouting extends MethodAlg {

  case class Method(value: String) {
    val extract: (RequestHeader) => Option[Unit] =
      (request: RequestHeader) =>
        if (request.method == value) Some(())
        else None
  }

  def Get = Method("GET")

  def Post = Method("POST")

  def Put = Method("PUT")

  def Delete = Method("DELETE")
}

