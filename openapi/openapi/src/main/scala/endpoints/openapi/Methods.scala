package endpoints
package openapi

/**
  * Interpreter for [[endpoints.algebra.Methods]]
  *
  * @group interpreters
  */
trait Methods extends endpoints.algebra.Methods {

  sealed trait Method
  case object Get extends Method
  case object Post extends Method
  case object Put extends Method
  case object Delete extends Method
  case object Options extends Method
  case object Patch extends Method


}
