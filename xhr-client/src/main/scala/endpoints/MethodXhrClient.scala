package endpoints

/**
  * Created by wpitula on 1/9/17.
  */
trait MethodXhrClient extends MethodAlg {
  type Method = String

  def Get: String = "GET"

  def Post: String = "POST"

  def Put: String = "PUT"

  def Delete: String = "DELETE"
}
