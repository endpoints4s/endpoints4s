package endpoints.algebra

/**
  * @group algebras
  */
trait StatusCodes {

  /** HTTP Status Code */
  type StatusCode

  // 2xx Success
  def OK: StatusCode

  def Created: StatusCode

  def Accepted: StatusCode

  def NoContent: StatusCode

  // 4xx Client Error
  /**
    * @note You should use the `badRequest` constructor provided by the [[endpoints.algebra.Responses]]
    *       trait to ensure that errors produced by ''endpoints'' are consistently
    *       handled by interpreters.
    */
  def BadRequest: StatusCode

  def Unauthorized: StatusCode

  def Forbidden: StatusCode

  def NotFound: StatusCode

  // 5xx Server Error
  /**
    * @note You should use the `internalServerError` constructor provided by the
    *       [[endpoints.algebra.Responses]] trait to ensure that errors produced by ''endpoints''
    *       are consistently handled by interpreters.
    */
  def InternalServerError: StatusCode

  def NotImplemented: StatusCode

}
