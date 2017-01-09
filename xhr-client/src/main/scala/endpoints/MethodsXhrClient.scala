package endpoints

/**
  * Created by wpitula on 1/9/17.
  */
trait MethodsXhrClient extends MethodsAlg {
  override type Method = String

  override def Get: String = "GET"

  override def Post: String = "POST"

  override def Put: String = "PUT"

  override def Delete: String = "DELETE"
}
