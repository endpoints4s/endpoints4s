package endpoints4s.sttp.client

import endpoints4s.algebra
import _root_.sttp.model.{StatusCode => SStatusCode}

/** @group interpreters
  */
trait StatusCodes extends algebra.StatusCodes {

  type StatusCode = SStatusCode

  def OK = SStatusCode.Ok
  def Created = SStatusCode.Created
  def Accepted = SStatusCode.Accepted
  override def NonAuthoritativeInformation = SStatusCode.NonAuthoritativeInformation
  def NoContent = SStatusCode.NoContent
  override def ResetContent = SStatusCode.ResetContent
  override def PartialContent = SStatusCode.PartialContent
  override def MultiStatus = SStatusCode.MultiStatus
  override def AlreadyReported = SStatusCode.AlreadyReported
  override def IMUsed = SStatusCode.ImUsed

  override def NotModified = SStatusCode.NotModified

  def BadRequest = SStatusCode.BadRequest
  def Unauthorized = SStatusCode.Unauthorized
  override def PaymentRequired = SStatusCode.PaymentRequired
  def Forbidden = SStatusCode.Forbidden
  def NotFound = SStatusCode.NotFound
  override def MethodNotAllowed = SStatusCode.MethodNotAllowed
  override def NotAcceptable = SStatusCode.NotAcceptable
  override def ProxyAuthenticationRequired = SStatusCode.ProxyAuthenticationRequired
  override def RequestTimeout = SStatusCode.RequestTimeout
  override def Conflict = SStatusCode.Conflict
  override def Gone = SStatusCode.Gone
  override def LengthRequired = SStatusCode.LengthRequired
  override def PreconditionFailed = SStatusCode.PreconditionFailed
  def PayloadTooLarge = SStatusCode.PayloadTooLarge
  override def UriTooLong = SStatusCode.UriTooLong
  override def UnsupportedMediaType = SStatusCode.UnsupportedMediaType
  override def RangeNotSatisfiable = SStatusCode.RangeNotSatisfiable
  override def ExpectationFailed = SStatusCode.ExpectationFailed
  override def MisdirectedRequest = SStatusCode.MisdirectedRequest
  override def UnprocessableEntity = SStatusCode.UnprocessableEntity
  override def Locked = SStatusCode.Locked
  override def FailedDependency = SStatusCode.FailedDependency
  override def UpgradeRequired = SStatusCode.UpgradeRequired
  override def PreconditionRequired = SStatusCode.PreconditionRequired
  def TooManyRequests = SStatusCode.TooManyRequests
  override def RequestHeaderFieldsTooLarge = SStatusCode.RequestHeaderFieldsTooLarge
  override def UnavailableForLegalReasons = SStatusCode.UnavailableForLegalReasons

  def InternalServerError = SStatusCode.InternalServerError
  def NotImplemented = SStatusCode.NotImplemented

}
