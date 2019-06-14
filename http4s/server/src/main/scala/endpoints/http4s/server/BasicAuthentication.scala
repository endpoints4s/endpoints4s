package endpoints.http4s.server

import cats.implicits._
import endpoints.algebra.BasicAuthentication.Credentials
import endpoints.algebra.Documentation
import org.http4s
import org.http4s.{BasicCredentials, Status}
import org.http4s.headers.Authorization

trait BasicAuthentication[F[_]]
    extends Endpoints[F]
    with endpoints.algebra.BasicAuthentication {

  private[endpoints] def basicAuthenticationHeader
    : RequestHeaders[Credentials] = { headers =>
    headers.get(Authorization).flatMap { authHeader =>
      authHeader.credentials match {
        case BasicCredentials(username, password) =>
          Some(Credentials(username, password))
        case _ => None
      }
    }
  }

  private[endpoints] def authenticated[A](
      response: Response[A],
      docs: Documentation): Response[Option[A]] = {
    case Some(a) => response(a)
    case None =>
      http4s.Response[F](status = Status.Forbidden).pure[F]
  }
}
