package endpoints
package openapi

/**
  * Interpreter for [[endpoints.algebra.StatusCodes]]
  *
  * @group interpreters
  */
trait StatusCodes extends endpoints.algebra.StatusCodes {

  type StatusCode = Int

  def OK = 200

  def BadRequest = 400

  def Unauthorized = 401

  def NotFound = 404

}
