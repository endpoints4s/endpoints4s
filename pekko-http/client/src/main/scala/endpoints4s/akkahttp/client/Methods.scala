package endpoints4s.akkahttp.client

import akka.http.scaladsl.model.{HttpMethods, HttpRequest}
import endpoints4s.algebra

/** @group interpreters
  */
trait Methods extends algebra.Methods {
  type Method = HttpRequest => HttpRequest

  def Get = _.withMethod(HttpMethods.GET)

  def Post = _.withMethod(HttpMethods.POST)

  def Put = _.withMethod(HttpMethods.PUT)

  def Delete = _.withMethod(HttpMethods.DELETE)

  def Options = _.withMethod(HttpMethods.OPTIONS)

  def Patch = _.withMethod(HttpMethods.PATCH)
}
