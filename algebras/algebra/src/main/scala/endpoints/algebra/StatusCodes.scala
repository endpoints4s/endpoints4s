package endpoints.algebra

/**
  * @group algebras
  */
trait StatusCodes {

  /** HTTP Status Code */
  type StatusCode

  /** 2xx Success */
  def OK: StatusCode

  /** 4xx Client Error */
  def BadRequest: StatusCode

  def Unauthorized: StatusCode

  def NotFound: StatusCode

}
