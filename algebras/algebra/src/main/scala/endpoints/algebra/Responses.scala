package endpoints.algebra

import endpoints.{InvariantFunctor, InvariantFunctorSyntax}

/**
  * @group algebras
  */
trait Responses extends StatusCodes with InvariantFunctorSyntax { this: Errors =>

  /** An HTTP response (status, headers, and entity) carrying an information of type A
    *
    * @note This type has implicit methods provided by the [[InvariantFunctorSyntax]]
    *       and [[ResponseSyntax]] class
    * @group types
    */
  type Response[A]

  /** Provides the operation `xmap` to the type `Response`
    * @see [[InvariantFunctorSyntax]]
    */
  implicit def responseInvFunctor: InvariantFunctor[Response]

  /** An HTTP response entity carrying an information of type A
    * @group types
    */
  type ResponseEntity[A]

  /**
    * Empty response entity
    * @group operations
    */
  def emptyResponse: ResponseEntity[Unit]

  /**
    * Text response entity
    * @group operations
    */
  def textResponse: ResponseEntity[String]

  /**
    * Server interpreters construct a response with the given status and entity.
    * Client interpreters accept a response only if it has a corresponding status code.
    *
    * @param statusCode Response status code
    * @param entity     Response entity
    * @param docs       Response documentation
    * @group operations
    */
  def response[A](statusCode: StatusCode, entity: ResponseEntity[A], docs: Documentation = None): Response[A]

  /**
    * Alternative between two possible choices of responses.
    *
    * Server interpreters construct either one or the other response.
    * Client interpreters accept either one or the other response.
    * Documentation interpreters list all the possible responses.
    */
  def choiceResponse[A, B](responseA: Response[A], responseB: Response[B]): Response[Either[A, B]]

  /**
    * OK (200) Response with the given entity
    * @group operations
    */
  final def ok[A](entity: ResponseEntity[A], docs: Documentation = None): Response[A] =
    response(OK, entity, docs)

  /**
    * Bad Request (400) response, with an entity of type `ClientErrors`.
    * @see [[endpoints.algebra.Errors]] and [[endpoints.algebra.BuiltInErrors]]
    * @group operations
    */
  final def badRequest(docs: Documentation = None): Response[ClientErrors] =
    response(BadRequest, clientErrorsResponseEntity, docs)

  /**
    * Internal Server Error (500) response, with an entity of type `ServerError`.
    * @see [[endpoints.algebra.Errors]] and [[endpoints.algebra.BuiltInErrors]]
    * @group operations
    */
  final def internalServerError(docs: Documentation = None): Response[ServerError] =
    response(InternalServerError, serverErrorResponseEntity, docs)

  /**
    * Turns a `Response[A]` into a `Response[Option[A]]`.
    *
    * Interpreters represent `None` with
    * an empty HTTP response whose status code is 404 (Not Found).
    *
    * @group operations
    */
  final def wheneverFound[A](responseA: Response[A], notFoundDocs: Documentation = None): Response[Option[A]] =
    responseA.orElse(response(NotFound, emptyResponse, notFoundDocs))
      .xmap(_.fold[Option[A]](Some(_), _ => None))(_.toLeft(()))

  /** Extension methods for [[Response]].
    *
    * @group operations
    */
  implicit class ResponseSyntax[A](response: Response[A]) {
    /**
      * Turns a `Response[A]` into a `Response[Option[A]]`.
      *
      * Interpreters represent `None` with
      * an empty HTTP response whose status code is 404 (Not Found).
      */
    final def orNotFound(notFoundDocs: Documentation = None): Response[Option[A]] = wheneverFound(response, notFoundDocs)

    /**
      * Alternative between two possible choices of responses.
      *
      * Server interpreters construct either one or the other response.
      * Client interpreters accept either one or the other response.
      * Documentation interpreters list all the possible responses.
      */
    final def orElse[B](otherResponse: Response[B]): Response[Either[A, B]] = choiceResponse(response, otherResponse)
  }

}
