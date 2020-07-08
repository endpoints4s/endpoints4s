package endpoints4s.http4s.client

import endpoints4s.algebra
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
  def NonAuthoritativeInformation = Http4sStatus.NonAuthoritativeInformation
  def NoContent = Http4sStatus.NoContent
  def ResetContent = Http4sStatus.ResetContent
  def PartialContent = Http4sStatus.PartialContent
  def MultiStatus = Http4sStatus.MultiStatus
  def AlreadyReported = Http4sStatus.AlreadyReported
  def IMUsed = Http4sStatus.IMUsed

  def BadRequest = Http4sStatus.BadRequest
  def Unauthorized = Http4sStatus.Unauthorized
  def PaymentRequired = Http4sStatus.PaymentRequired
  def Forbidden = Http4sStatus.Forbidden
  def NotFound = Http4sStatus.NotFound
  def MethodNotAllowed = Http4sStatus.MethodNotAllowed
  def NotAcceptable = Http4sStatus.NotAcceptable
  def ProxyAuthenticationRequired = Http4sStatus.ProxyAuthenticationRequired
  def RequestTimeout = Http4sStatus.RequestTimeout
  def Conflict = Http4sStatus.Conflict
  def Gone = Http4sStatus.Gone
  def LengthRequired = Http4sStatus.LengthRequired
  def PreconditionFailed = Http4sStatus.PreconditionFailed
  def PayloadTooLarge = Http4sStatus.PayloadTooLarge
  def UriTooLong = Http4sStatus.UriTooLong
  def UnsupportedMediaType = Http4sStatus.UnsupportedMediaType
  def RangeNotSatisfiable = Http4sStatus.RangeNotSatisfiable
  def ExpectationFailed = Http4sStatus.ExpectationFailed
  def MisdirectedRequest = Http4sStatus.MisdirectedRequest
  def UnprocessableEntity = Http4sStatus.UnprocessableEntity
  def Locked = Http4sStatus.Locked
  def FailedDependency = Http4sStatus.FailedDependency
  def TooEarly = Http4sStatus.TooEarly
  def UpgradeRequired = Http4sStatus.UpgradeRequired
  def PreconditionRequired = Http4sStatus.PreconditionRequired
  def TooManyRequests = Http4sStatus.TooManyRequests
  def RequestHeaderFieldsTooLarge = Http4sStatus.RequestHeaderFieldsTooLarge
  def UnavailableForLegalReasons = Http4sStatus.UnavailableForLegalReasons
  
  def InternalServerError = Http4sStatus.InternalServerError
  def NotImplemented = Http4sStatus.NotImplemented
}
