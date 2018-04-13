package endpoints.algebra

import scala.language.higherKinds

trait Responses {

  /** Information carried by a response */
  type Response[A]

  /**
    * Empty response.
    */
  def emptyResponse(docs: Documentation = None): Response[Unit]

  /**
    * Text response.
    */
  def textResponse(docs: Documentation = None): Response[String]

  /**
    * Turns a `Response[A]` into a `Response[Option[A]]`.
    *
    * Concrete interpreters should represent `None` with
    * an empty HTTP response whose status code is 404 (Not Found).
    */
  def option[A](response: Response[A], notFoundDocs: Documentation = None): Response[Option[A]]


}
