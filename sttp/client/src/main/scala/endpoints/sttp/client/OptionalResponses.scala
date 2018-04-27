package endpoints.sttp.client

import com.softwaremill.sttp

import scala.language.higherKinds

trait OptionalResponses[R[_]] extends endpoints.algebra.OptionalResponses { self: Endpoints[R] =>

    def option[A](inner: Response[A]): Response[Option[A]] = new SttpResponse[Option[A]] {
      override type ReceivedBody = inner.ReceivedBody
      override def responseAs = inner.responseAs
      override def validateResponse(response: sttp.Response[inner.ReceivedBody]): R[Option[A]] = {
        if (response.code == 404) backend.responseMonad.unit(None)
        else backend.responseMonad.map(inner.validateResponse(response))(Some(_))
      }
    }
}
