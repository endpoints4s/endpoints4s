package endpoints
package documented
package delegate

/**
  * Interpreter for [[algebra.JsonSchemaEntities]] that ignores
  * information related to documentation and delegates to another
  * [[endpoints.algebra.JsonEntitiesFromCodec]] interpreter.
  */
trait JsonSchemaEntities extends algebra.JsonSchemaEntities with Endpoints {

  val delegate: endpoints.algebra.JsonEntitiesFromCodec

  import delegate.jsonCodec

  def jsonRequest[A : JsonSchema](documentation: Option[String]): delegate.RequestEntity[A] =
    delegate.jsonRequest[A]

  def jsonResponse[A : JsonSchema](documentation: String): delegate.Response[A] =
    delegate.jsonResponse[A]

  implicit def jsonSchemaToDelegateJsonCodec[A : JsonSchema]: delegate.JsonCodec[A]

}
