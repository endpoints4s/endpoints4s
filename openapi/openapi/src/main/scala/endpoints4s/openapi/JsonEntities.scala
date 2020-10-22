package endpoints4s.openapi

import endpoints4s.openapi.model.MediaType
import endpoints4s.algebra

/** Partial interpreter for [[algebra.JsonEntities]].
  *
  * This interpreter documents that entities have a JSON content type, but
  * it can not document the schemas of these entities. See [[algebra.JsonEntitiesFromSchemas]]
  * for this purpose.
  *
  * @group interpreters
  */
trait JsonEntities extends algebra.JsonEntities with EndpointsWithCustomErrors {

  def jsonRequest[A: JsonRequest]: RequestEntity[A] =
    Map("application/json" -> MediaType(None))

  def jsonResponse[A: JsonResponse]: ResponseEntity[A] =
    Map("application/json" -> MediaType(None))

}

/** Interpreter for [[algebra.JsonEntitiesFromSchemas]] that produces a documentation of the JSON schemas.
  *
  * @group interpreters
  */
trait JsonEntitiesFromSchemas
    extends algebra.JsonEntitiesFromSchemas
    with EndpointsWithCustomErrors
    with JsonSchemas {

  def jsonRequest[A](implicit codec: JsonSchema[A]) =
    Map("application/json" -> MediaType(Some(toSchema(codec.docs))))

  def jsonResponse[A](implicit codec: JsonSchema[A]) =
    Map("application/json" -> MediaType(Some(toSchema(codec.docs))))

}
