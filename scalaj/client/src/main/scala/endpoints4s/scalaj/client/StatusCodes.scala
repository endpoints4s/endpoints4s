package endpoints4s.scalaj.client

import endpoints4s.algebra

/**
  * @group interpreters
  */
trait StatusCodes extends algebra.StatusCodes {

  type StatusCode = Int

  def OK = 200
  def Created = 201
  def Accepted = 202
  def NoContent = 204

  def BadRequest = 400
  def Unauthorized = 401
  def Forbidden = 403
  def NotFound = 404
  def PayloadTooLarge = 413
  def TooManyRequests = 429

  def InternalServerError = 500
  def NotImplemented = 501

}
