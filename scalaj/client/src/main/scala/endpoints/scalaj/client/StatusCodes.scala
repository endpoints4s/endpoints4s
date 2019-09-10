package endpoints.scalaj.client

import endpoints.algebra

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

  def InternalServerError = 500

}
