package endpoints4s.scalaj.client

import endpoints4s.algebra

/** @group interpreters
  */
trait StatusCodes extends algebra.StatusCodes {

  type StatusCode = Int

  def OK = 200
  def Created = 201
  def Accepted = 202
  override def NonAuthoritativeInformation = 203
  def NoContent = 204
  override def ResetContent = 205
  override def PartialContent = 206
  override def MultiStatus = 207
  override def AlreadyReported = 208
  override def IMUsed = 226

  override def NotModified = 304

  def BadRequest = 400
  def Unauthorized = 401
  override def PaymentRequired = 402
  def Forbidden = 403
  def NotFound = 404
  override def MethodNotAllowed = 405
  override def NotAcceptable = 406
  override def ProxyAuthenticationRequired = 407
  override def RequestTimeout = 408
  override def Conflict = 409
  override def Gone = 410
  override def LengthRequired = 411
  override def PreconditionFailed = 412
  def PayloadTooLarge = 413
  override def UriTooLong = 414
  override def UnsupportedMediaType = 415
  override def RangeNotSatisfiable = 416
  override def ExpectationFailed = 417
  override def MisdirectedRequest = 421
  override def UnprocessableEntity = 422
  override def Locked = 423
  override def FailedDependency = 424
  override def TooEarly = 425
  override def UpgradeRequired = 426
  override def PreconditionRequired = 428
  override def TooManyRequests = 429
  override def RequestHeaderFieldsTooLarge = 431
  override def UnavailableForLegalReasons = 451

  def InternalServerError = 500
  def NotImplemented = 501

}
