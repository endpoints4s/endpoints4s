package endpoints4s.xhr.faithful

import endpoints4s.algebra.MuxRequest
import endpoints4s.{Decoder, Encoder, xhr}
import faithful.{Future, Promise}

import scala.scalajs.js
import scala.scalajs.js.|

/** @group interpreters
  */
trait MuxEndpoints extends xhr.MuxEndpoints with Endpoints {

  class MuxEndpoint[Req <: MuxRequest, Resp, Transport](
      request: Request[Transport],
      response: Response[Transport]
  ) {
    def apply(
        req: Req
    )(implicit
        encoder: Encoder[Req, Transport],
        decoder: Decoder[Transport, Resp]
    ): (Future[req.Response], js.Function1[Unit, Unit]) = {
      val promise = new Promise[req.Response]()
      val (value, abort) = muxPerformXhr(request, response, req)
      value.`then`(
        (b: req.Response) => promise.success(b): Unit | js.Thenable[Unit],
        js.defined((e: Any) => {
          e match {
            case th: Throwable => promise.failure(th)
            case _             => promise.failure(js.JavaScriptException(e))
          }
          (): Unit | js.Thenable[Unit]
        }): js.UndefOr[
          js.Function1[Any, Unit | js.Thenable[Unit]]
        ]
      )
      (promise.future, abort)
    }
  }

  def muxEndpoint[Req <: MuxRequest, Resp, Transport](
      request: Request[Transport],
      response: Response[Transport]
  ): MuxEndpoint[Req, Resp, Transport] =
    new MuxEndpoint[Req, Resp, Transport](request, response)

}
