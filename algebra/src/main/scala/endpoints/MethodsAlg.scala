package endpoints


trait MethodsAlg {

  type Method

  def Get: Method

  def Post: Method

  def Put: Method

  def Delete: Method

}
