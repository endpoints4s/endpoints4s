package endpoints.scalaj.client

import endpoints.algebra

/**
  * @group interpreters
  */
trait StatusCodes extends algebra.StatusCodes {

  type StatusCode = Int

  def OK = 200
  def Created = 201
  def Accepted = 202
  def NonAuthoritativeInformation = 203
  def NoContent = 204
  def ResetContent = 205
  def PartialContent = 206
  def MultiStatus = 207
  def AlreadyReported = 208
  def IMUsed = 226

  def BadRequest = 400
  def Unauthorized = 401
  def PaymentRequired = 202
  def Forbidden = 403
  def NotFound = 404
  def MethodNotAllowed = 405
  def NotAcceptable = 406
  def ProxyAuthenticationRequired = 407
  def RequestTimeout = 408
  def Conflict = 409
  def Gone = 410
  def LengthRequired = 411
  def PreconditionFailed = 412
  def PayloadTooLarge = 413
  def UriTooLong = 414
  def UnsupportedMediaType = 415
  def RangeNotSatisfiable = 416
  def ExpectationFailed = 417
  def MisdirectedRequest = 421
  def UnprocessableEntity = 422
  def Locked = 423
  def FailedDependency = 424
  def TooEarly = 425
  def UpgradeRequired = 426
  def PreconditionRequired = 428
  def TooManyRequests = 429
  def RequestHeaderFieldsTooLarge = 431
  def UnavailableForLegalReasons = 451

  def InternalServerError = 500
  def NotImplemented = 501

}
