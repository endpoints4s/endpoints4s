package endpoints.algebra

import scala.language.higherKinds

trait Responses {

  /** Information carried by a response */
  type Response[A]

  /**
    * Empty response.
    */
  def emptyResponse(documentation: String): Response[Unit]

  /**
    * Text response.
    */
  def textResponse(documentation: String): Response[String]

}
