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
  def NoContent: StatusCode

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
  def Forbidden: StatusCode

  /** @group operations */
  def NotFound: StatusCode

  /** @group operations */
  def PayloadTooLarge: StatusCode

  /** @group operations */
  def TooManyRequests: StatusCode

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
