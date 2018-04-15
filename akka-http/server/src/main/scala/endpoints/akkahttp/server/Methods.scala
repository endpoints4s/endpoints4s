package endpoints.akkahttp.server

import akka.http.scaladsl.model.{HttpMethod, HttpMethods}
import endpoints.algebra

/**
  * [[algebra.Methods]] interpreter that decodes and encodes methods.
  */
trait Methods extends algebra.Methods {

  type Method = HttpMethod

  def Get = HttpMethods.GET

  def Post = HttpMethods.POST

  def Put = HttpMethods.PUT

  def Delete = HttpMethods.DELETE

  def Options = HttpMethods.OPTIONS

  def Patch = HttpMethods.PATCH
}

