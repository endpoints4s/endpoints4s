package endpoints
package openapi

import endpoints.algebra.Documentation
import endpoints.openapi.model.MediaType

/**
  * Interpreter for [[algebra.Responses]]
  *
  * @group interpreters
  */
trait Responses
  extends algebra.Responses
  with StatusCodes { this: algebra.Errors =>

  type ResponseEntity[A] = Map[String, MediaType]

  type Response[A] = List[DocumentedResponse]

  /**
    * @param status Response status code (e.g. OK or NotFound)
    * @param documentation Human readable documentation. Not optional because its required by openapi
    * @param content Map that associates each possible content-type (e.g. “text/html”) with a `MediaType` description
    */
  case class DocumentedResponse(status: StatusCode, documentation: String, content: Map[String, MediaType])

  implicit lazy val responseInvFunctor: InvariantFunctor[Response] =
    new InvariantFunctor[Response] {
      def xmap[A, B](fa: Response[A], f: A => B, g: B => A): Response[B] = fa
    }

  def emptyResponse: ResponseEntity[Unit] = Map.empty

  def textResponse: ResponseEntity[String] = Map("text/plain" -> MediaType(Some(model.Schema.simpleString)))

  def response[A](statusCode: StatusCode, entity: ResponseEntity[A], docs: Documentation = None): Response[A] =
    DocumentedResponse(statusCode, docs.getOrElse(""), entity) :: Nil

  def choiceResponse[A, B](responseA: Response[A], responseB: Response[B]): Response[Either[A, B]] =
    responseA ++ responseB

}
