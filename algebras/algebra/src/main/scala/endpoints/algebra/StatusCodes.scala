package endpoints.algebra

/**
  * @group algebras
  */
trait StatusCodes {

  /** HTTP Status Code */
  type StatusCode

  /** 2xx Success */
  def OK: StatusCode

  def Created: StatusCode

  def Accepted: StatusCode

  def NoContent: StatusCode

  /** 4xx Client Error */
  def BadRequest: StatusCode

  def Unauthorized: StatusCode

  def Forbidden: StatusCode

  def NotFound: StatusCode

  /** 5xx Server Error */
  def InternalServerError: StatusCode

}
