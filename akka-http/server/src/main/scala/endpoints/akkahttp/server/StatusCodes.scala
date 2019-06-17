package endpoints.akkahttp.server

import akka.http.scaladsl.model.{StatusCode => AkkaStatusCode, StatusCodes => AkkaStatusCodes}
import endpoints.algebra

/**
  * [[algebra.StatusCodes]] interpreter that decodes and encodes methods.
  *
  * @group interpreters
  */
trait StatusCodes extends algebra.StatusCodes {

  type StatusCode = AkkaStatusCode

  def OK = AkkaStatusCodes.OK

  def Unauthorized = AkkaStatusCodes.Unauthorized

  def NotFound = AkkaStatusCodes.NotFound

  def BadRequest= AkkaStatusCodes.BadRequest

}
