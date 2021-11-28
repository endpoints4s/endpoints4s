package endpoints4s.fetch

import endpoints4s.algebra
import org.scalajs.dom.{HttpMethod => FetchHttpMethod}

trait Methods extends algebra.Methods {
  type Method = FetchHttpMethod

  def Get: Method = FetchHttpMethod.GET

  def Post: Method = FetchHttpMethod.POST

  def Put: Method = FetchHttpMethod.PUT

  def Delete: Method = FetchHttpMethod.DELETE

  def Options: Method = FetchHttpMethod.OPTIONS

  def Patch: Method = FetchHttpMethod.PATCH
}
