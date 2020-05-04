package endpoints.akkahttp.client

import akka.http.scaladsl.model.{
  StatusCode => AkkaStatusCode,
  StatusCodes => AkkaStatusCodes
}
import endpoints.algebra

/**
  * [[algebra.StatusCodes]] interpreter that decodes and encodes methods.
  *
  * @group interpreters
  */
trait StatusCodes extends algebra.StatusCodes {

  type StatusCode = AkkaStatusCode

  def OK = AkkaStatusCodes.OK
  def Created = AkkaStatusCodes.Created
  def Accepted = AkkaStatusCodes.Accepted
  def NoContent = AkkaStatusCodes.NoContent

  def BadRequest = AkkaStatusCodes.BadRequest
  def Unauthorized = AkkaStatusCodes.Unauthorized
  def Forbidden = AkkaStatusCodes.Forbidden
  def NotFound = AkkaStatusCodes.NotFound
  def PayloadTooLarge = AkkaStatusCodes.PayloadTooLarge
  def TooManyRequests = AkkaStatusCodes.TooManyRequests

  def InternalServerError = AkkaStatusCodes.InternalServerError
  def NotImplemented = AkkaStatusCodes.NotImplemented

}
