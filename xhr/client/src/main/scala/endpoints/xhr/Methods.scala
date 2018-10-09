package endpoints.xhr

import endpoints.algebra

/**
  * @group interpreters
  */
trait Methods extends algebra.Methods {
  type Method = String

  def Get: String = "GET"

  def Post: String = "POST"

  def Put: String = "PUT"

  def Delete: String = "DELETE"

  def Options: String = "OPTIONS"

  def Patch: String = "PATCH"
}
