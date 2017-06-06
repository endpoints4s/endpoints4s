package endpoints.scalaj.client

import endpoints.algebra

trait Methods extends algebra.Methods {

  override type Method = String

  override def Get: Method = "GET"

  override def Post: Method = "POST"

  override def Put: Method = "PUT"

  override def Delete: Method = "DELETE"

}
