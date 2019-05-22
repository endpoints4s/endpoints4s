package endpoints.algebra

import scala.language.higherKinds

/**
  * @group algebras
  */
trait Responses extends StatusCodes {

  /** An HTTP response (status, headers, and entity) carrying an information of type A */
  type Response[A]

  /** An HTTP response entity carrying an information of type A */
  type ResponseEntity[A]

  /**
    * Empty response entity
    */
  def emptyResponse: ResponseEntity[Unit]

  /**
    * Text response entity
    */
  def textResponse: ResponseEntity[String]

  /**
    * @param statusCode Response status code
    * @param entity     Response entity
    * @param docs       Response documentation
    *
    * Server interpreters should construct a response with the given status and entity.
    * Client interpreters should accept a response only if it has a corresponding status code.
    */
  def response[A](statusCode: StatusCode, entity: ResponseEntity[A], docs: Documentation = None): Response[A]

  /**
    * OK Response with the given entity
    */
  final def ok[A](entity: ResponseEntity[A], docs: Documentation = None): Response[A] =
    response(OK, entity, docs)

  /**
    * Turns a `Response[A]` into a `Response[Option[A]]`.
    *
    * Concrete interpreters should represent `None` with
    * an empty HTTP response whose status code is 404 (Not Found).
    */
  def wheneverFound[A](response: Response[A], notFoundDocs: Documentation = None): Response[Option[A]]

  /** Extensions for [[Response]]. */
  implicit class ResponseExtensions[A](response: Response[A]) {
    /** syntax for `wheneverFound` */
    final def orNotFound(notFoundDocs: Documentation = None): Response[Option[A]] = wheneverFound(response, notFoundDocs)
  }

}
