package endpoints.http4s.client

import endpoints.algebra
import org.http4s

/**
  * [[algebra.Methods]] interpreter that decodes and encodes methods.
  *
  * @group interpreters
  */
trait Methods extends algebra.Methods {
  type Method = http4s.Method

  def Get: Method = http4s.Method.GET
  def Post: Method = http4s.Method.POST
  def Put: Method = http4s.Method.PUT
  def Delete: Method = http4s.Method.DELETE
  def Patch: Method = http4s.Method.PATCH
  def Options: Method = http4s.Method.OPTIONS
}
