package endpoints4s.algebra

import endpoints4s.Invalid

/** Defines the error types used to model client and server errors.
  *
  * The `ClientErrors` type is used by endpoints4s to model errors coming
  * from the client (missing query parameter, invalid entity, etc.).
  *
  * The `ServerError` type is used by endpoints4s to model errors coming from
  * the server business logic.
  *
  * The `badRequest` and `internalServerError` operations defined in [[Responses]]
  * define responses carrying entities of type `ClientErrors` and `ServerError`,
  * respectively.
  *
  * Interpreters are expected to use the `clientErrorsResponse` and `serverErrorResponse`
  * operations defined here to handle client and server errors, respectively.
  *
  * @see [[BuiltInErrors]]
  * @group algebras
  * @groupname types Types
  * @groupdesc types Types introduced by the algebra
  * @groupprio types 1
  * @groupname operations Operations
  * @groupdesc operations Operations creating and transforming values
  * @groupprio operations 2
  */
trait Errors { this: Responses =>

  /** Errors in a request built by a client
    * @group types
    */
  type ClientErrors

  /** Error raised by the business logic of a server
    * @group types
    */
  type ServerError

  /** Convert the endpoints4s internal client error type into the [[ClientErrors]] type
    * @group operations
    */
  def invalidToClientErrors(invalid: Invalid): ClientErrors

  /** Convert the [[ClientErrors]] type into the endpoints4s internal client error type
    * @group operations
    */
  def clientErrorsToInvalid(clientErrors: ClientErrors): Invalid

  /** Convert the endpoints4s internal server error type into the [[ServerError]] type
    * @group operations
    */
  def throwableToServerError(throwable: Throwable): ServerError

  /** Convert the [[ServerError]] type into the endpoints4s internal server error type
    * @group operations
    */
  def serverErrorToThrowable(serverError: ServerError): Throwable

  /** Response used by endpoints4s when decoding
    * a request fails.
    *
    * The provided implementation forwards to `badRequest`.
    *
    * @group operations
    */
  lazy val clientErrorsResponse: Response[ClientErrors] =
    badRequest(docs = Some("Client error"))

  /** Format of the response entity carrying the client errors.
    * @group operations
    */
  def clientErrorsResponseEntity: ResponseEntity[ClientErrors]

  /** Response used by endpoints4s when the
    * business logic of an endpoint fails.
    *
    * The provided implementation forwards to `internalServerError`
    * @group operations
    */
  lazy val serverErrorResponse: Response[ServerError] =
    internalServerError(docs = Some("Server error"))

  /** Format of the response entity carrying the server error.
    * @group operations
    */
  def serverErrorResponseEntity: ResponseEntity[ServerError]

}
