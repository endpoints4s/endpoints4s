package endpoints.scalaj.client

import endpoints.algebra

trait StatusCodes extends algebra.StatusCodes {

  type StatusCode = Int

  def OK = 200

  def BadRequest = 400

  def Unauthorized = 401

  def NotFound = 404

}
