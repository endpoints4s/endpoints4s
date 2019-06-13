package endpoints.http4s.server

import cats.effect.Sync
import endpoints.algebra.BasicAuthentication.Credentials
import endpoints.algebra.Documentation
import org.http4s.BasicCredentials
import org.http4s.headers.Authorization

abstract class BasicAuthentication[F[_]](implicit F: Sync[F])
    extends Endpoints
    with endpoints.algebra.BasicAuthentication {
  private[endpoints] def basicAuthenticationHeader
    : RequestHeaders[Credentials] =
    _.get(Authorization).flatMap { authHeader =>
      authHeader.credentials match {
        case BasicCredentials(username, password) =>
          Some(Credentials(username, password))
        case _ => None
      }
    }

  private[endpoints] def authenticated[A](
      response: Response[A],
      docs: Documentation): Response[Option[A]] = {
    case Some(a) => response(a)
    case None =>
      F.pure(org.http4s.Response(status = org.http4s.Status.Forbidden))
  }
}
