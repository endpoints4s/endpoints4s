package endpoints.akkahttp.client

import akka.http.scaladsl.model.{HttpMethods, HttpRequest}
import endpoints.algebra

trait Methods extends algebra.Methods {
  type Method = HttpRequest => HttpRequest

  def Get = _.copy(method = HttpMethods.GET)

  def Post = _.copy(method = HttpMethods.POST)

  def Put = _.copy(method = HttpMethods.PUT)

  def Delete = _.copy(method = HttpMethods.DELETE)

  def Patch = _.copy(method = HttpMethods.PATCH)

}