package endpoints4s.xhr

import endpoints4s.{Decoder, Encoder, algebra}
import endpoints4s.algebra.MuxRequest
import org.scalajs.dom.XMLHttpRequest

/** @group interpreters
  */
trait MuxEndpoints extends algebra.MuxEndpoints with EndpointsWithCustomErrors {

  protected final def muxPerformXhr[Req <: MuxRequest, Resp, Transport](
      request: Request[Transport],
      response: Response[Transport],
      req: Req
  )(
      onload: Either[Throwable, req.Response] => Unit,
      onError: XMLHttpRequest => Unit
  )(implicit
      encoder: Encoder[Req, Transport],
      decoder: Decoder[Transport, Resp]
  ): Unit =
    performXhr(request, response, encoder.encode(req))(
      errorOrResp =>
        onload(
          errorOrResp.flatMap(
            decoder.decode(_).asInstanceOf[Either[Throwable, req.Response]]
          )
        ),
      onError
    )

}
