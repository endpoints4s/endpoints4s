package endpoints.akkahttp.server

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
  def NonAuthoritativeInformation = AkkaStatusCodes.NonAuthoritativeInformation
  def NoContent = AkkaStatusCodes.NoContent
  def ResetContent = AkkaStatusCodes.ResetContent
  def PartialContent = AkkaStatusCodes.PartialContent
  def MultiStatus = AkkaStatusCodes.MultiStatus
  def AlreadyReported = AkkaStatusCodes.AlreadyReported
  def IMUsed = AkkaStatusCodes.IMUsed

  def BadRequest = AkkaStatusCodes.BadRequest
  def Unauthorized = AkkaStatusCodes.Unauthorized
  def PaymentRequired = AkkaStatusCodes.PaymentRequired
  def Forbidden = AkkaStatusCodes.Forbidden
  def NotFound = AkkaStatusCodes.NotFound
  def MethodNotAllowed = AkkaStatusCodes.MethodNotAllowed
  def NotAcceptable = AkkaStatusCodes.NotAcceptable
  def ProxyAuthenticationRequired = AkkaStatusCodes.ProxyAuthenticationRequired
  def RequestTimeout = AkkaStatusCodes.RequestTimeout
  def Conflict = AkkaStatusCodes.Conflict
  def Gone = AkkaStatusCodes.Gone
  def LengthRequired = AkkaStatusCodes.LengthRequired
  def PreconditionFailed = AkkaStatusCodes.PreconditionFailed
  def PayloadTooLarge = AkkaStatusCodes.PayloadTooLarge
  def UriTooLong = AkkaStatusCodes.UriTooLong
  def UnsupportedMediaType = AkkaStatusCodes.UnsupportedMediaType
  def RangeNotSatisfiable = AkkaStatusCodes.RangeNotSatisfiable
  def ExpectationFailed = AkkaStatusCodes.ExpectationFailed
  def MisdirectedRequest = AkkaStatusCodes.MisdirectedRequest
  def UnprocessableEntity = AkkaStatusCodes.UnprocessableEntity
  def Locked = AkkaStatusCodes.Locked
  def FailedDependency = AkkaStatusCodes.FailedDependency
  def TooEarly = AkkaStatusCodes.TooEarly
  def UpgradeRequired = AkkaStatusCodes.UpgradeRequired
  def PreconditionRequired = AkkaStatusCodes.PreconditionRequired
  def TooManyRequests = AkkaStatusCodes.TooManyRequests
  def RequestHeaderFieldsTooLarge = AkkaStatusCodes.RequestHeaderFieldsTooLarge
  def UnavailableForLegalReasons = AkkaStatusCodes.UnavailableForLegalReasons
  
  def InternalServerError = AkkaStatusCodes.InternalServerError
  def NotImplemented = AkkaStatusCodes.NotImplemented

}
