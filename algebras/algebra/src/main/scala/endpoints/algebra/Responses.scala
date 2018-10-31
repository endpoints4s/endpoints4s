package endpoints.algebra

import scala.language.higherKinds

/**
  * @group algebras
  */
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
  def wheneverFound[A](response: Response[A], notFoundDocs: Documentation = None): Response[Option[A]]

  /** Extensions for [[Response]]. */
  implicit class ResponseExtensions[A](response: Response[A]) {
    /** syntax for `wheneverFound` */
    final def orNotFound(notFoundDocs: Documentation = None): Response[Option[A]] = wheneverFound(response, notFoundDocs)
  }

}
