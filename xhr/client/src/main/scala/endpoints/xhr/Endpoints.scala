package endpoints.xhr

import endpoints.algebra
import endpoints.Tupler
import endpoints.algebra.{Decoder, Encoder, MuxRequest}
import org.scalajs.dom.XMLHttpRequest

import scala.language.higherKinds
import scala.scalajs.js

/**
  * Interpreter for [[algebra.Endpoints]] that builds a client issuing requests
  * using XMLHttpRequest.
  */
trait Endpoints extends algebra.Endpoints with Urls with Methods {

  /**
    * A function that takes the information `A` and the XMLHttpRequest
    * and sets up some headers on it.
    */
  type RequestHeaders[A] = js.Function2[A, XMLHttpRequest, Unit]

  /** Sets up no headers on the given XMLHttpRequest */
  lazy val emptyHeaders: RequestHeaders[Unit] = (_, _) => ()

  /**
    * A function that takes the information `A` and returns an XMLHttpRequest
    * with an optional request entity. If provided, the request entity must be
    * compatible with the `send` method of XMLHttpRequest.
    */
  // FIXME Use a representation that makes it easier to set the request Content-Type header according to its entity type
  type Request[A] = js.Function1[A, (XMLHttpRequest, Option[js.Any])]

  /**
    * A function that, given information `A` and an XMLHttpRequest, returns
    * a request entity.
    * Also, as a side-effect, the function can set the corresponding Content-Type header
    * on the given XMLHttpRequest.
    */
  type RequestEntity[A] = js.Function2[A, XMLHttpRequest, js.Any]

  lazy val emptyRequest: RequestEntity[Unit] = (_,_) => null

  def request[A, B, C, AB](method: Method, url: Url[A], entity: RequestEntity[B], headers: RequestHeaders[C])(implicit tuplerAB: Tupler.Aux[A, B, AB], tuplerABC: Tupler[AB, C]): Request[tuplerABC.Out] =
    (abc: tuplerABC.Out) => {
      val (ab, c) = tuplerABC.unapply(abc)
      val (a, b) = tuplerAB.unapply(ab)
      val xhr = makeXhr(method, url, a, headers, c)
      (xhr, Some(entity(b, xhr)))
    }

  private def makeXhr[A, B](method: String, url: Url[A], a: A, headers: RequestHeaders[B], b: B): XMLHttpRequest = {
    val xhr = new XMLHttpRequest
    xhr.open(method, url.encode(a))
    headers(b, xhr)
    xhr
  }

  /**
    * Attempts to decode an `A` from an XMLHttpRequest’s response
    */
  type Response[A] = js.Function1[XMLHttpRequest, Either[Exception, A]]

  /**
    * Successfully decodes no information from a response
    */
  lazy val emptyResponse: Response[Unit] = _ => Right(())

  /**
    * Successfully decodes string information from a response
    */
  lazy val textResponse: Response[String] = x => Right(x.responseText)

  /**
    * A function that takes the information needed to build a request and returns
    * a task yielding the information carried by the response.
    */
  type Endpoint[A, B] = js.Function1[A, Result[B]]

  /**
    * A value that eventually yields an `A`.
    *
    * Typically, concrete representation of `Result` will have an instance of `MonadError`, so
    * that we can perform requests (sequentially and in parallel) and recover errors.
    */
  type Result[A]

  protected final def performXhr[A, B](
    request: Request[A],
    response: Response[B],
    a: A
  )(onload: Either[Exception, B] => Unit, onerror: XMLHttpRequest => Unit): Unit = {
    val (xhr, maybeEntity) = request(a)
    xhr.onload = _ => onload(response(xhr))
    xhr.onerror = _ => onerror(xhr)
    xhr.send(maybeEntity.orNull)
  }

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
      errorOrResp => onload(errorOrResp.right.flatMap(decoder.decode(_).asInstanceOf[Either[Throwable, req.Response]])),
      onError
    )

}
