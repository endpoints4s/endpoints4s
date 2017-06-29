package endpoints
package documented
package delegate

/**
  * Interpreter for [[algebra.JsonEntities]] that ignores
  * information related to documentation and delegates to another
  * [[endpoints.algebra.JsonEntities]] interpreter.
  */
trait JsonEntities
  extends algebra.JsonEntities
    with Endpoints {

  val delegate: endpoints.algebra.JsonEntities

  type JsonRequest[A] = delegate.JsonRequest[A]

  def jsonRequest[A : JsonRequest](description: Option[String]): delegate.RequestEntity[A] = delegate.jsonRequest[A]

  type JsonResponse[A] = delegate.JsonResponse[A]

  def jsonResponse[A : JsonResponse](description: String): delegate.Response[A] = delegate.jsonResponse[A]

}
