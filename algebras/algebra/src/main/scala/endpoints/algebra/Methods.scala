package endpoints.algebra

trait Methods {

  type Method

  def Get: Method

  def Post: Method

  def Put: Method

  def Delete: Method

  def Patch: Method

}
