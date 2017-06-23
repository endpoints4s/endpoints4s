package endpoints
package delegate

/**
  * Interpreter for [[algebra.DocumentedJsonEntities]] that ignores
  * information related to documentation and delegates to another
  * [[algebra.JsonEntities]] interpreter.
  */
trait DocumentedJsonEntities
  extends algebra.DocumentedJsonEntities
    with DocumentedEndpoints {

  val delegate: algebra.JsonEntities

  type JsonRequest[A] = delegate.JsonRequest[A]

  def jsonRequest[A : JsonRequest](description: Option[String]): delegate.RequestEntity[A] = delegate.jsonRequest[A]

  type JsonResponse[A] = delegate.JsonResponse[A]

  def jsonResponse[A : JsonResponse](description: String): delegate.Response[A] = delegate.jsonResponse[A]

}
