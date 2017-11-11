package endpoints.akkahttp.client

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers._
import endpoints.algebra
import endpoints.algebra.BasicAuthentication.Credentials

import scala.concurrent.Future

trait BasicAuthentication extends algebra.BasicAuthentication { self: Endpoints =>

  /**
    * Supplies the credential into the request headers
    */
  private[endpoints] lazy val basicAuthentication: RequestHeaders[Credentials] =
    (credentials, headers) => {
      headers :+ Authorization(BasicHttpCredentials(credentials.username, credentials.password))
    }

  /**
    * Checks that the result is not `Forbidden`
    */
  private[endpoints] def authenticated[A](response: Response[A]): Response[Option[A]] =
    resp =>
      if (resp.status == StatusCodes.Forbidden) Future.successful(Right(None))
      else response(resp).map(_.right.map(Some.apply))

}
