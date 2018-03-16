package endpoints.documented.delegate.circe

import endpoints.algebra.circe
import endpoints.documented.delegate.Endpoints

/**
  * Interpreter for [[endpoints.documented.algebra.circe.JsonEntitiesFromCodec]] that delegates to
  * an [[endpoints.algebra.circe.JsonEntitiesFromCodec]] interpreter.
  */
trait JsonEntitiesFromCodec
  extends Endpoints
    with endpoints.documented.algebra.circe.JsonEntitiesFromCodec {

  val delegate: circe.JsonEntitiesFromCodec

  def jsonRequest[A : JsonRequest](documentation: Option[String]): delegate.RequestEntity[A] = delegate.jsonRequest[A]

  def jsonResponse[A : JsonResponse](documentation: String): delegate.Response[A] = delegate.jsonResponse[A]

}
