package endpoints.xhr

import endpoints.algebra

/**
  * Created by wpitula on 1/9/17.
  */
trait Methods extends algebra.Methods {
  type Method = String

  def Get: String = "GET"

  def Post: String = "POST"

  def Put: String = "PUT"

  def Delete: String = "DELETE"
}
