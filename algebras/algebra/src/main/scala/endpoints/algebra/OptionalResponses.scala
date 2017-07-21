package endpoints.algebra

/**
  * {{{
  *   /**
  *     * Describes an endpoint whose response ''might'' have an entity containing a `User`.
  *     */
  *   val example = endpoint(get(path / "user" / segment[UUID]), option(jsonResponse[User]))
  * }}}
  */
trait OptionalResponses extends Endpoints {

  /**
    * Turns a `Response[A]` into a `Response[Option[A]]`.
    *
    * Concrete interpreters should represent `None` with
    * an empty HTTP response whose status code is 404 (Not Found).
    */
  def option[A](response: Response[A]): Response[Option[A]]

}
