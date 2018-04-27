package endpoints.sttp.client

import com.softwaremill.sttp

import scala.language.higherKinds

trait OptionalResponses[R[_]] extends endpoints.algebra.OptionalResponses { self: Endpoints[R] =>

    def option[A](inner: Response[A]): Response[Option[A]] = new SttpResponse[Option[A]] {
      override type RB = inner.RB
      override def responseAs = inner.responseAs
      override def validateResponse(response: sttp.Response[inner.RB]): Either[String, Option[A]] = {
        if (response.code == 404) Right(None)
        else inner.validateResponse(response).right.map(Some(_))
      }
    }
}
