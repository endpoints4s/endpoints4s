package endpoints4s.http4s.client

import endpoints4s.algebra
import org.http4s.{Status => Http4sStatus}

/** [[algebra.StatusCodes]] interpreter that decodes and encodes methods.
  *
  * @group interpreters
  */
trait StatusCodes extends algebra.StatusCodes {

  type StatusCode = Http4sStatus

  def OK = Http4sStatus.Ok
  def Created = Http4sStatus.Created
  def Accepted = Http4sStatus.Accepted
  override def NonAuthoritativeInformation = Http4sStatus.NonAuthoritativeInformation
  def NoContent = Http4sStatus.NoContent
  override def ResetContent = Http4sStatus.ResetContent
  override def PartialContent = Http4sStatus.PartialContent
  override def MultiStatus = Http4sStatus.MultiStatus
  override def AlreadyReported = Http4sStatus.AlreadyReported
  override def IMUsed = Http4sStatus.IMUsed

  override def NotModified = Http4sStatus.NotModified
  def BadRequest = Http4sStatus.BadRequest
  def Unauthorized = Http4sStatus.Unauthorized
  override def PaymentRequired = Http4sStatus.PaymentRequired
  def Forbidden = Http4sStatus.Forbidden
  def NotFound = Http4sStatus.NotFound
  override def MethodNotAllowed = Http4sStatus.MethodNotAllowed
  override def NotAcceptable = Http4sStatus.NotAcceptable
  override def ProxyAuthenticationRequired = Http4sStatus.ProxyAuthenticationRequired
  override def RequestTimeout = Http4sStatus.RequestTimeout
  override def Conflict = Http4sStatus.Conflict
  override def Gone = Http4sStatus.Gone
  override def LengthRequired = Http4sStatus.LengthRequired
  override def PreconditionFailed = Http4sStatus.PreconditionFailed
  def PayloadTooLarge = Http4sStatus.PayloadTooLarge
  override def UriTooLong = Http4sStatus.UriTooLong
  override def UnsupportedMediaType = Http4sStatus.UnsupportedMediaType
  override def RangeNotSatisfiable = Http4sStatus.RangeNotSatisfiable
  override def ExpectationFailed = Http4sStatus.ExpectationFailed
  override def MisdirectedRequest = Http4sStatus.MisdirectedRequest
  override def UnprocessableEntity = Http4sStatus.UnprocessableEntity
  override def Locked = Http4sStatus.Locked
  override def FailedDependency = Http4sStatus.FailedDependency
  override def TooEarly = Http4sStatus.TooEarly
  override def UpgradeRequired = Http4sStatus.UpgradeRequired
  override def PreconditionRequired = Http4sStatus.PreconditionRequired
  def TooManyRequests = Http4sStatus.TooManyRequests
  override def RequestHeaderFieldsTooLarge = Http4sStatus.RequestHeaderFieldsTooLarge
  override def UnavailableForLegalReasons = Http4sStatus.UnavailableForLegalReasons

  def InternalServerError = Http4sStatus.InternalServerError
  def NotImplemented = Http4sStatus.NotImplemented
}
