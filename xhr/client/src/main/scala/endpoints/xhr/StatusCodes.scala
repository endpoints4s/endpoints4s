package endpoints.xhr

import endpoints.algebra

/**
  * [[algebra.StatusCodes]] interpreter that decodes and encodes methods.
  *
  * @group interpreters
  */
trait StatusCodes extends algebra.StatusCodes {

  type StatusCode = Int

  def OK = 200

  def BadRequest = 400

  def Unauthorized = 401

  def NotFound = 404

}
