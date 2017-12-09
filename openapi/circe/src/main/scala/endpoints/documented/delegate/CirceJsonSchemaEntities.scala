package endpoints
package documented
package delegate

/**
  * Interpreter for [[algebra.JsonSchemaEntities]] that ignores
  * information related to documentation and delegates to another
  * [[endpoints.algebra.CirceEntities]] interpreter.
  */
trait CirceJsonSchemaEntities
  extends algebra.JsonSchemaEntities
    with Endpoints
    with circe.JsonSchemas {

  val delegate: endpoints.algebra.CirceEntities

  def jsonRequest[A : JsonRequest](documentation: Option[String]): delegate.RequestEntity[A] =
    delegate.jsonRequest[A](JsonSchema.toCirceCodec)

  def jsonResponse[A : JsonResponse](documentation: String): delegate.Response[A] =
    delegate.jsonResponse[A](JsonSchema.toCirceCodec)

}
