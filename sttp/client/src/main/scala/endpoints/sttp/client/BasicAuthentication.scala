package endpoints.sttp.client

import com.softwaremill.sttp
import endpoints.algebra
import endpoints.algebra.BasicAuthentication.Credentials
import endpoints.algebra.Documentation

import scala.language.higherKinds

/**
  * @group interpreters
  */
trait BasicAuthentication[R[_]] extends algebra.BasicAuthentication { self: Endpoints[R] =>

  /**
    * Supplies the credential into the request headers
    */
  private[endpoints] lazy val basicAuthenticationHeader: RequestHeaders[Credentials] =
    (credentials, request) => {
      request.auth.basic(credentials.username, credentials.password)
    }

  /**
    * Checks that the result is not `Forbidden`
    */
  private[endpoints] def authenticated[A](inner: Response[A], docs: Documentation): Response[Option[A]] = {
    new Response[Option[A]] {
      val entity = mapResponseEntity[A, Option[A]](inner.entity)(Some(_))
      def decodeResponse(response: sttp.Response[inner.entity.ReceivedBody]): R[Option[A]] = {
        if (response.code == 403) backend.responseMonad.unit(None)
        else entity.decodeEntity(response)
      }
    }
  }
}
