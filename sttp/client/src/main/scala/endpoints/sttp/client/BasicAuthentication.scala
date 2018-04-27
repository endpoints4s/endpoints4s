package endpoints.sttp.client

import com.softwaremill.sttp
import endpoints.algebra
import endpoints.algebra.BasicAuthentication.Credentials

import scala.language.higherKinds

trait BasicAuthentication[R[_]] extends algebra.BasicAuthentication { self: Endpoints[R] =>

  /**
    * Supplies the credential into the request headers
    */
  private[endpoints] lazy val basicAuthentication: RequestHeaders[Credentials] =
    (credentials, request) => {
      request.auth.basic(credentials.username, credentials.password)
    }

  /**
    * Checks that the result is not `Forbidden`
    */
  private[endpoints] def authenticated[A](inner: Response[A]): Response[Option[A]] = {
    new SttpResponse[Option[A]] {
      override type RB = inner.RB
      override def responseAs = inner.responseAs
      override def validateResponse(response: sttp.Response[inner.RB]): Either[String, Option[A]] = {
        if (response.code == 403) Right(None)
        else inner.validateResponse(response).right.map(Some(_))
      }
    }
  }
}
