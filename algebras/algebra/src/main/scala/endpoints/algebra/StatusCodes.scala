package endpoints.algebra

/**
  * @group algebras
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
  def NonAuthoritativeInformation: StatusCode

  /** @group operations */
  def NoContent: StatusCode

  /** @group operations */
  def ResetContent: StatusCode

  /** @group operations */
  def PartialContent: StatusCode

  /** @group operations */
  def MultiStatus: StatusCode

  /** @group operations */
  def AlreadyReported: StatusCode

  /** @group operations */
  def IMUsed: StatusCode
  

  // 4xx Client Error
  /**
    * @note You should use the `badRequest` constructor provided by the [[endpoints.algebra.Responses]]
    *       trait to ensure that errors produced by ''endpoints'' are consistently
    *       handled by interpreters.
    * @group operations
    */
  def BadRequest: StatusCode

  /** @group operations */
  def Unauthorized: StatusCode

  /** @group operations */
  def PaymentRequired: StatusCode

  /** @group operations */
  def Forbidden: StatusCode

  /** @group operations */
  def NotFound: StatusCode

  /** @group operations */
  def MethodNotAllowed: StatusCode

  /** @group operations */
  def NotAcceptable: StatusCode

  /** @group operations */
  def ProxyAuthenticationRequired: StatusCode

  /** @group operations */
  def RequestTimeout: StatusCode

  /** @group operations */
  def Conflict: StatusCode

  /** @group operations */
  def Gone: StatusCode

  /** @group operations */
  def LengthRequired: StatusCode

  /** @group operations */
  def PreconditionFailed: StatusCode

  /** @group operations */
  def PayloadTooLarge: StatusCode

  /** @group operations */
  def UriTooLong: StatusCode

  /** @group operations */
  def UnsupportedMediaType: StatusCode

  /** @group operations */
  def RangeNotSatisfiable: StatusCode

  /** @group operations */
  def ExpectationFailed: StatusCode

  /** @group operations */
  def MisdirectedRequest: StatusCode

  /** @group operations */
  def UnprocessableEntity: StatusCode

  /** @group operations */
  def Locked: StatusCode

  /** @group operations */
  def FailedDependency: StatusCode

  /** @group operations */
  def TooEarly: StatusCode

  /** @group operations */
  def UpgradeRequired: StatusCode

  /** @group operations */
  def PreconditionRequired: StatusCode

  /** @group operations */
  def TooManyRequests: StatusCode

  /** @group operations */
  def RequestHeaderFieldsTooLarge: StatusCode

  /** @group operations */
  def UnavailableForLegalReasons: StatusCode

  // 5xx Server Error
  /**
    * @note You should use the `internalServerError` constructor provided by the
    *       [[endpoints.algebra.Responses]] trait to ensure that errors produced by ''endpoints''
    *       are consistently handled by interpreters.
    * @group operations
    */
  def InternalServerError: StatusCode

  /** @group operations */
  def NotImplemented: StatusCode

}
