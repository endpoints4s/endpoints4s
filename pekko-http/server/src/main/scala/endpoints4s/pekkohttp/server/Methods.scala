package endpoints4s.pekkohttp.server

import endpoints4s.algebra
import org.apache.pekko.http.scaladsl.model.{HttpMethod, HttpMethods}

/** [[algebra.Methods]] interpreter that decodes and encodes methods.
  *
  * @group interpreters
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
