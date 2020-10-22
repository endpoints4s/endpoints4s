package endpoints4s.algebra

import endpoints4s.Invalid

/** Interpreter for the [[Errors]] algebra that uses endpoints4s built-in error types:
  *
  *   - [[Invalid]] for client errors,
  *   - and `Throwable` for server error.
  *
  * Both types of errors are serialized into a JSON array containing string error values.
  *
  * @group interpreters
  */
trait BuiltInErrors extends Errors { this: EndpointsWithCustomErrors =>

  type ClientErrors = Invalid
  type ServerError = Throwable

  final def invalidToClientErrors(invalid: Invalid): ClientErrors = invalid
  final def clientErrorsToInvalid(clientErrors: ClientErrors): Invalid =
    clientErrors

  final def throwableToServerError(throwable: Throwable): ServerError =
    throwable
  final def serverErrorToThrowable(serverError: ServerError): Throwable =
    serverError

  /** Response entity format for [[Invalid]] values
    */
  def clientErrorsResponseEntity: ResponseEntity[Invalid]

  /** Response entity format for `Throwable` values
    */
  def serverErrorResponseEntity: ResponseEntity[Throwable]

}
