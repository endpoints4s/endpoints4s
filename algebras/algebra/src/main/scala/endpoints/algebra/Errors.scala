package endpoints.algebra

import endpoints.Invalid

/**
  * Defines the error types used to model client and server errors.
  *
  * The `ClientErrors` type is used by ''endpoints'' to model errors coming
  * from the client (missing query parameter, invalid entity, etc.).
  *
  * The `ServerError` type is used by ''endpoints'' to model errors coming from
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
  */
trait Errors { this: Responses =>

  /** Errors in a request built by a client */
  type ClientErrors
  /** Error raised by the business logic of a server */
  type ServerError

  def invalidToClientErrors(invalid: Invalid): ClientErrors
  def clientErrorsToInvalid(clientErrors: ClientErrors): Invalid

  def throwableToServerError(throwable: Throwable): ServerError
  def serverErrorToThrowable(serverError: ServerError): Throwable

  /**
    * Response used by the ''endpoints'' library when decoding
    * a request fails.
    *
    * The provided implementation forwards to `badRequest`.
    */
  lazy val clientErrorsResponse: Response[ClientErrors] = badRequest(docs = Some("Client error"))

  /**
    * Format of the response entity carrying the client errors.
    */
  def clientErrorsResponseEntity: ResponseEntity[ClientErrors]

  /**
    * Response used by the ''endpoints'' library when the
    * business logic of an endpoint fails.
    *
    * The provided implementation forwards to `internalServerError`
    */
  lazy val serverErrorResponse: Response[ServerError] = internalServerError(docs = Some("Server error"))

  /**
    * Format of the response entity carrying the server error.
    */
  def serverErrorResponseEntity: ResponseEntity[ServerError]

}
