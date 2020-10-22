package endpoints4s.algebra

/** @group algebras
  * @groupname types Types
  * @groupdesc types Types introduced by the algebra
  * @groupprio types 1
  * @groupname operations Operations
  * @groupdesc operations Operations creating and transforming values
  * @groupprio operations 2
  */
trait Methods {

  /** HTTP Method
    * @group types
    */
  type Method

  /** @group operations */
  def Get: Method

  /** @group operations */
  def Post: Method

  /** @group operations */
  def Put: Method

  /** @group operations */
  def Delete: Method

  /** @group operations */
  def Patch: Method

  /** @group operations */
  def Options: Method

}
