package endpoints4s.akkahttp.server

import akka.http.scaladsl.model.{StatusCode => AkkaStatusCode, StatusCodes => AkkaStatusCodes}
import endpoints4s.algebra

/** [[algebra.StatusCodes]] interpreter that decodes and encodes methods.
  *
  * @group interpreters
  */
trait StatusCodes extends algebra.StatusCodes {

  type StatusCode = AkkaStatusCode

  def OK = AkkaStatusCodes.OK
  def Created = AkkaStatusCodes.Created
  def Accepted = AkkaStatusCodes.Accepted
  override def NonAuthoritativeInformation = AkkaStatusCodes.NonAuthoritativeInformation
  def NoContent = AkkaStatusCodes.NoContent
  override def ResetContent = AkkaStatusCodes.ResetContent
  override def PartialContent = AkkaStatusCodes.PartialContent
  override def MultiStatus = AkkaStatusCodes.MultiStatus
  override def AlreadyReported = AkkaStatusCodes.AlreadyReported
  override def IMUsed = AkkaStatusCodes.IMUsed

  override def NotModified = AkkaStatusCodes.NotModified

  def BadRequest = AkkaStatusCodes.BadRequest
  def Unauthorized = AkkaStatusCodes.Unauthorized
  override def PaymentRequired = AkkaStatusCodes.PaymentRequired
  def Forbidden = AkkaStatusCodes.Forbidden
  def NotFound = AkkaStatusCodes.NotFound
  override def MethodNotAllowed = AkkaStatusCodes.MethodNotAllowed
  override def NotAcceptable = AkkaStatusCodes.NotAcceptable
  override def ProxyAuthenticationRequired = AkkaStatusCodes.ProxyAuthenticationRequired
  override def RequestTimeout = AkkaStatusCodes.RequestTimeout
  override def Conflict = AkkaStatusCodes.Conflict
  override def Gone = AkkaStatusCodes.Gone
  override def LengthRequired = AkkaStatusCodes.LengthRequired
  override def PreconditionFailed = AkkaStatusCodes.PreconditionFailed
  def PayloadTooLarge = AkkaStatusCodes.PayloadTooLarge
  override def UriTooLong = AkkaStatusCodes.UriTooLong
  override def UnsupportedMediaType = AkkaStatusCodes.UnsupportedMediaType
  override def RangeNotSatisfiable = AkkaStatusCodes.RangeNotSatisfiable
  override def ExpectationFailed = AkkaStatusCodes.ExpectationFailed
  override def MisdirectedRequest = AkkaStatusCodes.MisdirectedRequest
  override def UnprocessableEntity = AkkaStatusCodes.UnprocessableEntity
  override def Locked = AkkaStatusCodes.Locked
  override def FailedDependency = AkkaStatusCodes.FailedDependency
  override def TooEarly = AkkaStatusCodes.TooEarly
  override def UpgradeRequired = AkkaStatusCodes.UpgradeRequired
  override def PreconditionRequired = AkkaStatusCodes.PreconditionRequired
  def TooManyRequests = AkkaStatusCodes.TooManyRequests
  override def RequestHeaderFieldsTooLarge = AkkaStatusCodes.RequestHeaderFieldsTooLarge
  override def UnavailableForLegalReasons = AkkaStatusCodes.UnavailableForLegalReasons

  def InternalServerError = AkkaStatusCodes.InternalServerError
  def NotImplemented = AkkaStatusCodes.NotImplemented

}
