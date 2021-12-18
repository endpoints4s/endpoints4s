package endpoints4s.xhr

import endpoints4s.algebra.MuxRequest
import endpoints4s.Decoder
import endpoints4s.Encoder
import endpoints4s.algebra

import scala.scalajs.js

/** @group interpreters
  */
trait MuxEndpoints extends algebra.MuxEndpoints with EndpointsWithCustomErrors with FutureLike {

  protected final def muxPerformXhr[Req <: MuxRequest, Resp, Transport](
      request: Request[Transport],
      response: Response[Transport],
      req: Req
  )(implicit
      encoder: Encoder[Req, Transport],
      decoder: Decoder[Transport, Resp]
  ): (FutureLike[req.Response], js.Function0[Unit]) = {
    val (value, abort) = performXhr(request, response, encoder.encode(req))

    (
      value.flatMap((b: Transport) => {
        futureLike[req.Response] { (resolve, error) =>
          decoder
            .decode(b)
            .asInstanceOf[Either[Throwable, req.Response]]
            .fold(
              th => error(th),
              r => resolve(r)
            )
        }
      }),
      abort
    )
  }

}
