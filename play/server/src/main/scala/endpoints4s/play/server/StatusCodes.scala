package endpoints4s.play.server

import endpoints4s.algebra
import play.api.mvc.Results

/** @group interpreters
  */
trait StatusCodes extends algebra.StatusCodes {

  type StatusCode = Results.Status

  def OK = Results.Ok
  def Created = Results.Created
  def Accepted = Results.Accepted
  override def NonAuthoritativeInformation = Results.Status(203)
  def NoContent = Results.Status(204)
  override def ResetContent = Results.Status(205)
  override def PartialContent = Results.Status(206)
  override def MultiStatus = Results.Status(207)
  override def AlreadyReported = Results.Status(208)
  override def IMUsed = Results.Status(226)

  override def NotModified = Results.Status(304)

  def BadRequest = Results.BadRequest
  def Unauthorized = Results.Unauthorized
  override def PaymentRequired = Results.Status(402)
  def Forbidden = Results.Forbidden
  def NotFound = Results.NotFound
  override def MethodNotAllowed = Results.Status(405)
  override def NotAcceptable = Results.Status(406)
  override def ProxyAuthenticationRequired = Results.Status(407)
  override def RequestTimeout = Results.Status(408)
  override def Conflict = Results.Status(409)
  override def Gone = Results.Status(410)
  override def LengthRequired = Results.Status(411)
  override def PreconditionFailed = Results.Status(412)
  def PayloadTooLarge = Results.EntityTooLarge
  override def UriTooLong = Results.Status(414)
  override def UnsupportedMediaType = Results.Status(415)
  override def RangeNotSatisfiable = Results.Status(416)
  override def ExpectationFailed = Results.Status(417)
  override def MisdirectedRequest = Results.Status(421)
  override def UnprocessableEntity = Results.Status(422)
  override def Locked = Results.Status(423)
  override def FailedDependency = Results.Status(424)
  override def TooEarly = Results.Status(425)
  override def UpgradeRequired = Results.Status(426)
  override def PreconditionRequired = Results.Status(428)
  def TooManyRequests = Results.TooManyRequests
  override def RequestHeaderFieldsTooLarge = Results.Status(431)
  override def UnavailableForLegalReasons = Results.Status(451)

  def InternalServerError = Results.InternalServerError
  def NotImplemented = Results.NotImplemented

}
