package endpoints.sttp.client

import endpoints.algebra
import com.softwaremill.sttp

/**
  * [[algebra.Methods]] interpreter that builds URLs.
  *
  * @group interpreters
  */
trait Methods extends algebra.Methods {

  type Method = sttp.Request[_, Nothing] => sttp.Request[_, Nothing]

  private def setMethod(method: sttp.Method): Method = _.copy(method = method: sttp.Id[sttp.Method])

  def Get = setMethod(sttp.Method.GET)

  def Post = setMethod(sttp.Method.POST)

  def Put = setMethod(sttp.Method.PUT)

  def Delete = setMethod(sttp.Method.DELETE)

  def Patch = setMethod(sttp.Method.PATCH)

  def Options = setMethod(sttp.Method.OPTIONS)

}
