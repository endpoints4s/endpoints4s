package endpoints4s
package openapi

/** Interpreter for [[endpoints4s.algebra.Methods]]
  *
  * @group interpreters
  */
trait Methods extends endpoints4s.algebra.Methods {

  sealed trait Method
  case object Get extends Method
  case object Post extends Method
  case object Put extends Method
  case object Delete extends Method
  case object Options extends Method
  case object Patch extends Method

}
