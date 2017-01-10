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

  override def Get = Method("GET")

  override def Post = Method("POST")

  override def Put = Method("PUT")

  override def Delete = Method("DELETE")
}

