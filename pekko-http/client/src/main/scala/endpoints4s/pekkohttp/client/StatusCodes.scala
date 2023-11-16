package endpoints4s.pekkohttp.client

import org.apache.pekko.http.scaladsl.model.{StatusCode => PekkoStatusCode, StatusCodes => PekkoStatusCodes}
import endpoints4s.algebra

/** [[algebra.StatusCodes]] interpreter that decodes and encodes methods.
  *
  * @group interpreters
  */
trait StatusCodes extends algebra.StatusCodes {

  type StatusCode = PekkoStatusCode

  def OK = PekkoStatusCodes.OK
  def Created = PekkoStatusCodes.Created
  def Accepted = PekkoStatusCodes.Accepted
  override def NonAuthoritativeInformation = PekkoStatusCodes.NonAuthoritativeInformation
  def NoContent = PekkoStatusCodes.NoContent
  override def ResetContent = PekkoStatusCodes.ResetContent
  override def PartialContent = PekkoStatusCodes.PartialContent
  override def MultiStatus = PekkoStatusCodes.MultiStatus
  override def AlreadyReported = PekkoStatusCodes.AlreadyReported
  override def IMUsed = PekkoStatusCodes.IMUsed

  override def NotModified = PekkoStatusCodes.NotModified
  override def TemporaryRedirect = PekkoStatusCodes.TemporaryRedirect
  override def PermanentRedirect = PekkoStatusCodes.PermanentRedirect

  def BadRequest = PekkoStatusCodes.BadRequest
  def Unauthorized = PekkoStatusCodes.Unauthorized
  override def PaymentRequired = PekkoStatusCodes.PaymentRequired
  def Forbidden = PekkoStatusCodes.Forbidden
  def NotFound = PekkoStatusCodes.NotFound
  override def MethodNotAllowed = PekkoStatusCodes.MethodNotAllowed
  override def NotAcceptable = PekkoStatusCodes.NotAcceptable
  override def ProxyAuthenticationRequired = PekkoStatusCodes.ProxyAuthenticationRequired
  override def RequestTimeout = PekkoStatusCodes.RequestTimeout
  override def Conflict = PekkoStatusCodes.Conflict
  override def Gone = PekkoStatusCodes.Gone
  override def LengthRequired = PekkoStatusCodes.LengthRequired
  override def PreconditionFailed = PekkoStatusCodes.PreconditionFailed
  def PayloadTooLarge = PekkoStatusCodes.PayloadTooLarge
  override def UriTooLong = PekkoStatusCodes.UriTooLong
  override def UnsupportedMediaType = PekkoStatusCodes.UnsupportedMediaType
  override def RangeNotSatisfiable = PekkoStatusCodes.RangeNotSatisfiable
  override def ExpectationFailed = PekkoStatusCodes.ExpectationFailed
  override def MisdirectedRequest = PekkoStatusCodes.MisdirectedRequest
  override def UnprocessableEntity = PekkoStatusCodes.UnprocessableEntity
  override def Locked = PekkoStatusCodes.Locked
  override def FailedDependency = PekkoStatusCodes.FailedDependency
  override def TooEarly = PekkoStatusCodes.TooEarly
  override def UpgradeRequired = PekkoStatusCodes.UpgradeRequired
  override def PreconditionRequired = PekkoStatusCodes.PreconditionRequired
  def TooManyRequests = PekkoStatusCodes.TooManyRequests
  override def RequestHeaderFieldsTooLarge = PekkoStatusCodes.RequestHeaderFieldsTooLarge
  override def UnavailableForLegalReasons = PekkoStatusCodes.UnavailableForLegalReasons

  def InternalServerError = PekkoStatusCodes.InternalServerError
  def NotImplemented = PekkoStatusCodes.NotImplemented

}
