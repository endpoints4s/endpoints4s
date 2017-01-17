package endpoints.algebra

import endpoints.Tupler
import endpoints.algebra.BasicAuthentication.Credentials

/**
  * Provides vocabulary to define endpoints that use Basic HTTP authentication.
  *
  * This trait works fine, but developers are likely to implement their own
  * authentication mechanism, specific to their application.
  */
trait BasicAuthentication extends Endpoints {

  private[endpoints] def basicAuthentication: RequestHeaders[Credentials]

  private[endpoints] def authenticated[A](response: Response[A]): Response[Option[A]] // FIXME Use an extensible type to model authentication failure

  // TODO Allow users to supply additional `RequestHeaders`
  /**
    * Defines an endpoint protected by Basic HTTP authentication
    */
  def authenticatedEndpoint[A, B, C, AB](
    method: Method,
    url: Url[A],
    requestEntity: RequestEntity[B] = emptyRequest,
    response: Response[C]
  )(implicit
    tuplerAB: Tupler.Aux[A, B, AB],
    tuplerABC: Tupler[AB, Credentials]
  ): Endpoint[tuplerABC.Out, Option[C]] =
    endpoint(request(method, url, requestEntity, basicAuthentication), authenticated(response))

}

object BasicAuthentication {
  case class Credentials(username: String, password: String)
}

