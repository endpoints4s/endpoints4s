package endpoints.akkahttp.client

import akka.http.scaladsl.model.HttpResponse
import endpoints.algebra

import scala.concurrent.Future

trait OptionalResponses extends algebra.OptionalResponses { this: Endpoints =>

  override def option[A](inner: HttpResponse => Future[Either[Throwable, A]]): HttpResponse => Future[Either[Throwable, Option[A]]] = {
    {
      case resp if resp.status.intValue() == 404 => Future.successful(Right(None))
      case resp => inner(resp).map(_.right.map(Some(_)))
    }
  }
}
