package endpoints
package documented
package delegate

/**
  * Interpreter for [[algebra.CirceEntities]]
  * that ignores the documentation information and delegate
  * to an [[endpoints.algebra.CirceEntities]] interpreter.
  */
trait CirceEntities
  extends algebra.CirceEntities
    with Endpoints {

  val delegate: endpoints.algebra.CirceEntities

  def jsonRequest[A : JsonRequest](documentation: Option[String]): delegate.RequestEntity[A] = delegate.jsonRequest[A]

  def jsonResponse[A : JsonResponse](documentation: String): delegate.Response[A] = delegate.jsonResponse[A]

}
