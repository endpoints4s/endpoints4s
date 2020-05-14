package endpoints.http4s.client

import endpoints.algebra
import org.http4s.{Status => Http4sStatus}

/**
  * [[algebra.StatusCodes]] interpreter that decodes and encodes methods.
  *
  * @group interpreters
  */
trait StatusCodes extends algebra.StatusCodes {

  type StatusCode = Http4sStatus

  def OK = Http4sStatus.Ok
  def Created = Http4sStatus.Created
  def Accepted = Http4sStatus.Accepted
  def NoContent = Http4sStatus.NoContent
  def BadRequest = Http4sStatus.BadRequest
  def Unauthorized = Http4sStatus.Unauthorized
  def Forbidden = Http4sStatus.Forbidden
  def NotFound = Http4sStatus.NotFound
  def PayloadTooLarge = Http4sStatus.PayloadTooLarge
  def TooManyRequests = Http4sStatus.TooManyRequests
  def InternalServerError = Http4sStatus.InternalServerError
  def NotImplemented = Http4sStatus.NotImplemented
}
