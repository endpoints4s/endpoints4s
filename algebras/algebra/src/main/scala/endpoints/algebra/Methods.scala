package endpoints.algebra

/**
  * @group algebras
  */
trait Methods {

  /** HTTP Method */
  type Method

  def Get: Method

  def Post: Method

  def Put: Method

  def Delete: Method

  def Patch: Method

  def Options: Method

}
