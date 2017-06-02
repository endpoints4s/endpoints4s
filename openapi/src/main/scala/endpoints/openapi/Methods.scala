package endpoints
package openapi

/**
  * Interpreter for [[algebra.Methods]]
  */
trait Methods extends algebra.Methods {

  sealed trait Method
  case object Get extends Method
  case object Post extends Method
  case object Put extends Method
  case object Delete extends Method

}
