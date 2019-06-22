package endpoints.play.server

import endpoints.algebra

import play.api.mvc.Results

trait StatusCodes extends algebra.StatusCodes {

  type StatusCode = Results.Status

  def OK = Results.Ok

  def BadRequest = Results.BadRequest

  def Unauthorized = Results.Unauthorized

  def NotFound = Results.NotFound

}
