package endpoints
package algebra

import scala.language.higherKinds

/**
  * Algebra interface for describing responses.
  *
  * This interface is modeled after [[Responses]] but some
  * methods take additional parameters carrying documentation.
  */
trait DocumentedResponses {

  /** Information carried by a response */
  type Response[A]

  /**
    * Empty response.
    */
  def emptyResponse(description: String): Response[Unit]

  /**
    * Text response.
    */
  def textResponse(description: String): Response[String]

}
