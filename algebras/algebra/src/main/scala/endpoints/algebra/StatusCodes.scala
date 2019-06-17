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
  def Unauthorized: StatusCode

  def NotFound: StatusCode

  def BadRequest: StatusCode

}
