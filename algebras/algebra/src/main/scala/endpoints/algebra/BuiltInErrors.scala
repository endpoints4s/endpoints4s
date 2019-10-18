package endpoints.algebra

import endpoints.{Invalid, Valid, Validated}

/**
  * Uses ''endpoints'' built-in error types:
  *
  * - [[Invalid]] for client errors,
  *
  * - and `Throwable` for server error.
  *
  * Both types of errors are serialized into a JSON array containing string error values.
  */
trait BuiltInErrors extends Errors { this: EndpointsWithCustomErrors =>

  type ClientErrors = Invalid
  type ServerError = Throwable

  final def invalidToClientErrors(invalid: Invalid): ClientErrors = invalid
  final def clientErrorsToInvalid(clientErrors: ClientErrors): Invalid = clientErrors

  final def throwableToServerError(throwable: Throwable): ServerError = throwable
  final def serverErrorToThrowable(serverError: ServerError): Throwable = serverError

  /**
    * Response entity format for [[Invalid]] values
    */
  def clientErrorsResponseEntity: ResponseEntity[Invalid]

  /**
    * Response entity format for `Throwable` values
    */
  def serverErrorResponseEntity: ResponseEntity[Throwable]

}

object InvalidCodec {

  implicit val invalidCodec: Codec[String, Invalid] =
    // TODO Make JSON encoding and decoding more robust
    new Codec[String, Invalid] {
      def decode(from: String): Validated[Invalid] = {
        val errors =
          from.drop(2).dropRight(2).split("(?<!\\\\)\",\"")
            .map(error => error.replaceAllLiterally("\\\\", "\\").replaceAllLiterally("\\\"", "\"").replaceAllLiterally("\\n", "\n"))
            .toIndexedSeq
        Valid(Invalid(errors))
      }
      def encode(invalid: Invalid): String = {
        // Manually encode JSON because we don’t want to have a dependency on a JSON library here
        val jsonErrors =
          invalid.errors.map(error => s""""${error.replaceAllLiterally("\\", "\\\\").replaceAllLiterally("\"", "\\\"").replaceAllLiterally("\n", "\\n")}"""")
        s"[${jsonErrors.mkString(",")}]"
      }
    }

}
