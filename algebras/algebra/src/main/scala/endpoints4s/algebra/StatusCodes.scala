package endpoints4s.algebra

/** @group algebras
  * @groupname types Types
  * @groupdesc types Types introduced by the algebra
  * @groupprio types 1
  * @groupname operations Operations
  * @groupdesc operations Operations creating and transforming values
  * @groupprio operations 2
  */
trait StatusCodes {

  /** HTTP Status Code
    * @group types
    */
  type StatusCode

  // 2xx Success
  /** @group operations */
  def OK: StatusCode

  /** @group operations */
  def Created: StatusCode

  /** @group operations */
  def Accepted: StatusCode

  /** @group operations */
  def NonAuthoritativeInformation: StatusCode = unsupportedInterpreter("1.1.0")

  /** @group operations */
  def NoContent: StatusCode

  /** @group operations */
  def ResetContent: StatusCode = unsupportedInterpreter("1.1.0")

  /** @group operations */
  def PartialContent: StatusCode = unsupportedInterpreter("1.1.0")

  /** @group operations */
  def MultiStatus: StatusCode = unsupportedInterpreter("1.1.0")

  /** @group operations */
  def AlreadyReported: StatusCode = unsupportedInterpreter("1.1.0")

  /** @group operations */
  def IMUsed: StatusCode = unsupportedInterpreter("1.1.0")

  // 3xx Redirection
  /** @group operations */
  def NotModified: StatusCode = unsupportedInterpreter("1.2.0")

  // 4xx Client Error
  /** @note You should use the `badRequest` constructor provided by the [[endpoints4s.algebra.Responses]]
    *       trait to ensure that errors produced by endpoints4s are consistently
    *       handled by interpreters.
    * @group operations
    */
  def BadRequest: StatusCode

  /** @group operations */
  def Unauthorized: StatusCode

  /** @group operations */
  def PaymentRequired: StatusCode = unsupportedInterpreter("1.1.0")

  /** @group operations */
  def Forbidden: StatusCode

  /** @group operations */
  def NotFound: StatusCode

  /** @group operations */
  def MethodNotAllowed: StatusCode = unsupportedInterpreter("1.1.0")

  /** @group operations */
  def NotAcceptable: StatusCode = unsupportedInterpreter("1.1.0")

  /** @group operations */
  def ProxyAuthenticationRequired: StatusCode = unsupportedInterpreter("1.1.0")

  /** @group operations */
  def RequestTimeout: StatusCode = unsupportedInterpreter("1.1.0")

  /** @group operations */
  def Conflict: StatusCode = unsupportedInterpreter("1.1.0")

  /** @group operations */
  def Gone: StatusCode = unsupportedInterpreter("1.1.0")

  /** @group operations */
  def LengthRequired: StatusCode = unsupportedInterpreter("1.1.0")

  /** @group operations */
  def PreconditionFailed: StatusCode = unsupportedInterpreter("1.1.0")

  /** @group operations */
  def PayloadTooLarge: StatusCode

  /** @group operations */
  def UriTooLong: StatusCode = unsupportedInterpreter("1.1.0")

  /** @group operations */
  def UnsupportedMediaType: StatusCode = unsupportedInterpreter("1.1.0")

  /** @group operations */
  def RangeNotSatisfiable: StatusCode = unsupportedInterpreter("1.1.0")

  /** @group operations */
  def ExpectationFailed: StatusCode = unsupportedInterpreter("1.1.0")

  /** @group operations */
  def MisdirectedRequest: StatusCode = unsupportedInterpreter("1.1.0")

  /** @group operations */
  def UnprocessableEntity: StatusCode = unsupportedInterpreter("1.1.0")

  /** @group operations */
  def Locked: StatusCode = unsupportedInterpreter("1.1.0")

  /** @group operations */
  def FailedDependency: StatusCode = unsupportedInterpreter("1.1.0")

  /** @group operations */
  def TooEarly: StatusCode = unsupportedInterpreter("1.1.0")

  /** @group operations */
  def UpgradeRequired: StatusCode = unsupportedInterpreter("1.1.0")

  /** @group operations */
  def PreconditionRequired: StatusCode = unsupportedInterpreter("1.1.0")

  /** @group operations */
  def TooManyRequests: StatusCode

  /** @group operations */
  def RequestHeaderFieldsTooLarge: StatusCode = unsupportedInterpreter("1.1.0")

  /** @group operations */
  def UnavailableForLegalReasons: StatusCode = unsupportedInterpreter("1.1.0")

  // 5xx Server Error
  /** @note You should use the `internalServerError` constructor provided by the
    *       [[endpoints4s.algebra.Responses]] trait to ensure that errors produced by endpoints4s
    *       are consistently handled by interpreters.
    * @group operations
    */
  def InternalServerError: StatusCode

  /** @group operations */
  def NotImplemented: StatusCode

}
