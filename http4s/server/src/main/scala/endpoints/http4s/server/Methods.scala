package endpoints.http4s.server

import endpoints.algebra
import org.http4s

trait Methods extends algebra.Methods {
  type Method = http4s.Method

  override def Get: Method = http4s.Method.GET

  override def Post: Method = http4s.Method.POST

  override def Put: Method = http4s.Method.PUT

  override def Delete: Method = http4s.Method.DELETE

  override def Patch: Method = http4s.Method.PATCH

  override def Options: Method = http4s.Method.OPTIONS
}
