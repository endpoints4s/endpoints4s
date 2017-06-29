package endpoints
package documented
package algebra

import scala.language.higherKinds

/**
  * Algebra interface for describing responses.
  *
  * This interface is modeled after [[endpoints.algebra.Responses]] but some
  * methods take additional parameters carrying documentation.
  */
trait Responses {

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
