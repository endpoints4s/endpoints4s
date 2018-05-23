package endpoints.play.server

import endpoints.algebra
import play.api.mvc.RequestHeader

/**
  * [[algebra.Methods]] interpreter that decodes and encodes methods.
  */
trait Methods extends algebra.Methods {

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

  def Options = Method("OPTIONS")

  def Patch = Method("PATCH")
}

