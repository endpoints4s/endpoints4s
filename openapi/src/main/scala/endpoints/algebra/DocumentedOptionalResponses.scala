package endpoints
package algebra

/**
  * Algebra interface for describing optional responses including documentation.
  *
  * This interface is modeled after [[OptionalResponses]] but the `option`
  * method takes an additional parameter carrying a description.
  */
trait DocumentedOptionalResponses
  extends DocumentedEndpoints {

  /**
    * @return A description of an HTTP response that can be either 404 (Not found)
    *         or the underlying `response`.
    * @param response Underlying response
    * @param description Description in case there is no result
    */
  def option[A](response: Response[A], description: String): Response[Option[A]]

}
