package endpoints.play.client

import endpoints.algebra

trait StatusCodes extends algebra.StatusCodes {

  type StatusCode = Int

  def OK = 200

  def NotFound = 404

  def Unauthorized = 401

  def BadRequest = 404

}
