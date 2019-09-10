package endpoints.akkahttp.client

import akka.http.scaladsl.model.{StatusCodes => AkkaStatusCodes}
import akka.http.scaladsl.model.headers._
import endpoints.algebra
import endpoints.algebra.BasicAuthentication.Credentials
import endpoints.algebra.Documentation

import scala.concurrent.Future

/**
  * @group interpreters
  */
trait BasicAuthentication extends algebra.BasicAuthentication { self: Endpoints =>

  /**
    * Supplies the credential into the request headers
    */
  private[endpoints] lazy val basicAuthenticationHeader: RequestHeaders[Credentials] =
    (credentials, headers) => {
      headers :+ Authorization(BasicHttpCredentials(credentials.username, credentials.password))
    }

  /**
    * Checks that the result is not `Forbidden`
    */
  private[endpoints] def authenticated[A](response: Response[A], docs: Documentation): Response[Option[A]] =
    (status, headers) =>
      if (status == AkkaStatusCodes.Forbidden) discardingEntity(Future.successful(Right(None)))
      else entity => response(status, headers)(entity).map(_.right.map(Some.apply))

}
