package endpoints


trait MethodAlg {

  type Method

  def Get: Method

  def Post: Method

  def Put: Method

  def Delete: Method

}
