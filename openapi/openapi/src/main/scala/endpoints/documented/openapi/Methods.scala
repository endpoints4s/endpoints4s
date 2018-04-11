package endpoints
package documented
package openapi

/**
  * Interpreter for [[endpoints.algebra.Methods]]
  */
trait Methods extends endpoints.algebra.Methods {

  sealed trait Method
  case object Get extends Method
  case object Post extends Method
  case object Put extends Method
  case object Delete extends Method
  case object Patch extends Method

}
