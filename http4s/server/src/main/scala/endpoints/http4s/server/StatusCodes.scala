package endpoints.http4s.server


import endpoints.algebra
import org.http4s.{Status => Http4sStatus}

/**
  * [[algebra.StatusCodes]] interpreter that decodes and encodes methods.
  *
  * @group interpreters
  */
trait StatusCodes extends algebra.StatusCodes {

  type StatusCode = Http4sStatus

  def OK = Http4sStatus.Ok

  def BadRequest= Http4sStatus.BadRequest

  def Unauthorized = Http4sStatus.Unauthorized

  def NotFound = Http4sStatus.NotFound

}

