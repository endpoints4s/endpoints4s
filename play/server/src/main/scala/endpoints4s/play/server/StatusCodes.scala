package endpoints4s.play.server

import endpoints4s.algebra
import play.api.mvc.Results

/**
  * @group interpreters
  */
trait StatusCodes extends algebra.StatusCodes {

  type StatusCode = Results.Status

  def OK = Results.Ok
  def Created = Results.Created
  def Accepted = Results.Accepted
  def NoContent = Results.Status(204)
  def NonAuthoritativeInformation = Results.Status(203)
  def ResetContent = Results.Status(205)
  def PartialContent = Results.Status(206)
  def MultiStatus = Results.Status(207)
  def AlreadyReported = Results.Status(208)
  def IMUsed = Results.Status(226)

  def BadRequest = Results.BadRequest
  def Unauthorized = Results.Unauthorized
  def Forbidden = Results.Forbidden
  def NotFound = Results.NotFound
  def PayloadTooLarge = Results.EntityTooLarge
  def TooManyRequests = Results.TooManyRequests
  def PaymentRequired = Results.Status(402)
  def MethodNotAllowed = Results.Status(405)
  def NotAcceptable = Results.Status(406)
  def ProxyAuthenticationRequired = Results.Status(407)
  def RequestTimeout = Results.Status(408)
  def Conflict = Results.Status(409)
  def Gone = Results.Status(410)
  def LengthRequired = Results.Status(411)
  def PreconditionFailed = Results.Status(412)
  def UriTooLong = Results.Status(414)
  def UnsupportedMediaType = Results.Status(415)
  def RangeNotSatisfiable = Results.Status(416)
  def ExpectationFailed = Results.Status(417)
  def MisdirectedRequest = Results.Status(421)
  def UnprocessableEntity = Results.Status(422)
  def Locked = Results.Status(423)
  def FailedDependency = Results.Status(424)
  def TooEarly = Results.Status(425)
  def UpgradeRequired = Results.Status(426)
  def PreconditionRequired = Results.Status(428)
  def RequestHeaderFieldsTooLarge = Results.Status(431)
  def UnavailableForLegalReasons = Results.Status(451)

  def InternalServerError = Results.InternalServerError
  def NotImplemented = Results.NotImplemented

}
