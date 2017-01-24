package endpoints.akkahttp.routing

import akka.http.scaladsl.model.{HttpMethod, HttpMethods}
import endpoints.algebra

/**
  * [[algebra.Methods]] interpreter that decodes and encodes methods.
  */
trait Methods extends algebra.Methods {

  override type Method = HttpMethod

  def Get = HttpMethods.GET

  def Post = HttpMethods.POST

  def Put = HttpMethods.PUT

  def Delete = HttpMethods.DELETE
}

