package endpoints4s.sttp.client

import endpoints4s.algebra
import sttp.client.{Request => SRequest, Identity}
import sttp.model.{Method => SMethod}

/** [[algebra.Methods]] interpreter that builds URLs.
  *
  * @group interpreters
  */
trait Methods extends algebra.Methods {

  type Method = SRequest[_, Nothing] => SRequest[_, Nothing]

  private def setMethod(method: SMethod): Method =
    _.copy(method = method: Identity[SMethod])

  def Get = setMethod(SMethod.GET)

  def Post = setMethod(SMethod.POST)

  def Put = setMethod(SMethod.PUT)

  def Delete = setMethod(SMethod.DELETE)

  def Patch = setMethod(SMethod.PATCH)

  def Options = setMethod(SMethod.OPTIONS)

}
