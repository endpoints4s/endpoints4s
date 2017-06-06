package endpoints.scalaj.client

import endpoints.algebra.MuxRequest
import endpoints.algebra

import scala.concurrent.{ExecutionContext, Future}

trait Endpoints extends algebra.Endpoints with Requests with Responses {

  override type MuxEndpoint[A, B, Transport] = Nothing

  override def endpoint[A, B](request: Request[A], response: Response[B]): Endpoint[A, B] = {
    Endpoint(request, response)
  }

  override def muxEndpoint[Req <: MuxRequest, Resp, Transport](request: Request[Transport], response: Response[Transport]): MuxEndpoint[Req, Resp, Transport] =
    throw new UnsupportedOperationException("Not implemented")


  case class Endpoint[Req, Resp](request: Request[Req], response: Response[Resp]) {

    /**
      * This method just wraps a call in a Future and is not real async call
      */
    def callAsync(args: Req)(implicit ec: ExecutionContext): Future[Resp] =
      Future {
        call(args)
      }

    def call(args: Req): Resp = response(request(args).asString)
  }

}
