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

  /**
    * Turns a `Response[A]` into a `Response[Option[A]]`.
    *
    * Concrete interpreters should represent `None` with
    * an empty HTTP response whose status code is 404 (Not Found).
    */
  def option[A](response: Response[A], notFoundDocumentation: String = ""): Response[Option[A]]


}
