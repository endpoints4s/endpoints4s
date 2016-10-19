package endpoints

/**
  * Provides vocabulary to define endpoints that use Basic HTTP authentication.
  *
  * This trait works fine, but developers are likely to implement their own
  * authentication mechanism, specific to their application.
  */
trait BasicAuthenticationAlg extends EndpointAlg {

  private[endpoints] def basicAuthentication: RequestHeaders[Credentials]

  private[endpoints] def authenticated[A](response: Response[A]): Response[Option[A]] // FIXME Use an extensible type to model authentication failure

  // TODO Abstract over GET vs POST
  // TODO Allow users to supply additional `RequestHeaders`
  /**
    * Defines a GET endpoint protected by Basic HTTP authentication
    */
  def authenticatedGetEndpoint[A, B](
    url: Url[A],
    response: Response[B]
  )(implicit
    tupler: Tupler[A, Credentials]
  ): Endpoint[tupler.Out, Option[B]] =
    endpoint(get(url, basicAuthentication), authenticated(response))

  /**
    * Defines a POST endpoint protected by Basic HTTP authentication
    */
  def authenticatedPostEndpoint[A, B, C, AB](
    url: Url[A],
    requestEntity: RequestEntity[B],
    response: Response[C]
  )(implicit
    tuplerAB: Tupler.Aux[A, B, AB],
    tuplerABC: Tupler[AB, Credentials]
  ): Endpoint[tuplerABC.Out, Option[C]] =
    endpoint(post(url, requestEntity, basicAuthentication), authenticated(response))

}

case class Credentials(username: String, password: String)
