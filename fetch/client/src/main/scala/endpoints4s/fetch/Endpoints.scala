package endpoints4s.fetch

import endpoints4s.Decoder
import endpoints4s.Hashing
import endpoints4s.Invalid
import endpoints4s.InvariantFunctor
import endpoints4s.PartialInvariantFunctor
import endpoints4s.Semigroupal
import endpoints4s.Tupler
import endpoints4s.Valid
import endpoints4s.Validated
import endpoints4s.algebra
import endpoints4s.algebra.Documentation
import org.scalajs.dom.AbortController
import org.scalajs.dom.Fetch
import org.scalajs.dom.{Headers => FetchHeaders}
import org.scalajs.dom.{HttpMethod => FetchHttpMethod}
import org.scalajs.dom.{RequestInit => FetchRequestInit}
import org.scalajs.dom.{Response => FetchResponse}

import scala.concurrent.TimeoutException
import scala.scalajs.js
import scala.scalajs.js.Promise
import scala.scalajs.js.|

trait Endpoints extends algebra.Endpoints with EndpointsWithCustomErrors with BuiltInErrors

trait EndpointsWithCustomErrors
    extends algebra.EndpointsWithCustomErrors
    with Urls
    with Methods
    with StatusCodes {

  def settings: EndpointsSettings

  type RequestHeaders[A] = js.Function2[A, FetchRequestInit, Unit]

  lazy val emptyRequestHeaders: RequestHeaders[Unit] = (_, _) => ()

  implicit class RequestInitOps(requestInit: FetchRequestInit) {
    def setRequestHeader(name: String, value: String): Unit = {
      if (requestInit.headers.isEmpty) {
        requestInit.headers = new FetchHeaders()
      }
      requestInit.headers.asInstanceOf[FetchHeaders].set(name, value)
    }
  }

  def requestHeader(
      name: String,
      docs: endpoints4s.algebra.Documentation
  ): RequestHeaders[String] =
    (value, requestInit) => requestInit.setRequestHeader(name, value)

  def optRequestHeader(
      name: String,
      docs: endpoints4s.algebra.Documentation
  ): RequestHeaders[Option[String]] =
    (valueOpt, requestInit) => valueOpt.foreach(value => requestInit.setRequestHeader(name, value))

  implicit lazy val requestHeadersPartialInvariantFunctor: PartialInvariantFunctor[RequestHeaders] =
    new PartialInvariantFunctor[RequestHeaders] {
      override def xmapPartial[From, To](
          f: js.Function2[From, FetchRequestInit, Unit],
          map: From => Validated[To],
          contramap: To => From
      ): js.Function2[To, FetchRequestInit, Unit] =
        (to, xhr) => f(contramap(to), xhr)
    }

  implicit lazy val requestHeadersSemigroupal: Semigroupal[RequestHeaders] =
    new Semigroupal[RequestHeaders] {
      override def product[A, B](
          fa: js.Function2[A, FetchRequestInit, Unit],
          fb: js.Function2[B, FetchRequestInit, Unit]
      )(implicit
          tupler: Tupler[A, B]
      ): js.Function2[tupler.Out, FetchRequestInit, Unit] =
        (out, xhr) => {
          val (a, b) = tupler.unapply(out)
          fa(a, xhr)
          fb(b, xhr)
        }
    }

  trait Request[A] {
    def apply(a: A): RequestData

    def href(a: A): String
  }

  implicit def requestPartialInvariantFunctor: PartialInvariantFunctor[Request] =
    new PartialInvariantFunctor[Request] {
      def xmapPartial[A, B](
          fa: Request[A],
          f: A => Validated[B],
          g: B => A
      ): Request[B] =
        new Request[B] {
          def apply(b: B): RequestData = fa(g(b))
          def href(b: B): String = fa.href(g(b))
        }
    }

  type RequestEntity[A] = (A, FetchRequestInit) => Unit

  lazy val emptyRequest: RequestEntity[Unit] = (_, _) => ()

  lazy val textRequest: RequestEntity[String] = (body, requestInit) => {
    requestInit.setRequestHeader("Content-type", "text/plain; charset=utf8")
    requestInit.body = body
  }

  def choiceRequestEntity[A, B](
      requestEntityA: RequestEntity[A],
      requestEntityB: RequestEntity[B]
  ): RequestEntity[Either[A, B]] =
    (eitherAB, requestInit) =>
      eitherAB.fold(requestEntityA(_, requestInit), requestEntityB(_, requestInit))

  implicit lazy val requestEntityPartialInvariantFunctor: PartialInvariantFunctor[RequestEntity] =
    new PartialInvariantFunctor[RequestEntity] {
      def xmapPartial[From, To](
          f: (From, FetchRequestInit) => Unit,
          map: From => Validated[To],
          contramap: To => From
      ): (To, FetchRequestInit) => Unit =
        (to, xhr) => f(contramap(to), xhr)
    }

  def request[A, B, C, AB, Out](
      method: Method,
      url: Url[A],
      entity: RequestEntity[B],
      docs: Documentation,
      headers: RequestHeaders[C]
  )(implicit
      tuplerAB: Tupler.Aux[A, B, AB],
      tuplerABC: Tupler.Aux[AB, C, Out]
  ): Request[Out] =
    new Request[Out] {
      def apply(abc: Out) = {
        val (ab, c) = tuplerABC.unapply(abc)
        val (_, b) = tuplerAB.unapply(ab)
        RequestData(method, headers(c, _), requestInit => entity(b, requestInit))
      }

      def href(abc: Out) = {
        val (ab, _) = tuplerABC.unapply(abc)
        val (a, _) = tuplerAB.unapply(ab)
        url.encode(a)
      }
    }

  type Response[A] = js.Function1[FetchResponse, Option[ResponseEntity[A]]]

  implicit lazy val responseInvariantFunctor: InvariantFunctor[Response] =
    new InvariantFunctor[Response] {
      def xmap[A, B](fa: Response[A], f: A => B, g: B => A): Response[B] =
        xhr => fa(xhr).map(mapResponseEntity(_)(f))
    }

  type ResponseEntity[A] = js.Function1[FetchResponse, js.Promise[Either[Throwable, A]]]

  implicit def responseEntityInvariantFunctor: InvariantFunctor[ResponseEntity] =
    new InvariantFunctor[ResponseEntity] {
      def xmap[A, B](
          fa: ResponseEntity[A],
          f: A => B,
          g: B => A
      ): ResponseEntity[B] =
        mapResponseEntity(fa)(f)
    }

  private[fetch] def mapResponseEntity[A, B](
      entity: ResponseEntity[A]
  )(f: A => B): ResponseEntity[B] =
    mapPartialResponseEntity(entity)(a => Right(f(a)))

  private[fetch] def mapPartialResponseEntity[A, B](
      entity: ResponseEntity[A]
  )(f: A => Either[Throwable, B]): ResponseEntity[B] =
    response =>
      entity(response).`then`((responseEntity: Either[Throwable, A]) =>
        responseEntity.flatMap(f): Either[Throwable, B] | js.Thenable[Either[Throwable, B]]
      )

  def stringCodecResponse[A](implicit
      codec: Decoder[String, A]
  ): ResponseEntity[A] =
    _.text()
      .`then`((text: String) =>
        codec
          .decode(text)
          .fold(
            Right(_),
            errors => Left(new Exception(errors.mkString(". ")))
          ): Either[Throwable, A] | js.Thenable[Either[Throwable, A]]
      )

  def emptyResponse: ResponseEntity[Unit] = _ => Promise.resolve[Either[Throwable, Unit]](Right(()))

  def textResponse: ResponseEntity[String] = response =>
    response
      .text()
      .`then`((text: String) =>
        Right(text): Either[Throwable, String] | js.Thenable[Either[Throwable, String]]
      )

  type ResponseHeaders[A] = FetchResponse => Validated[A]

  implicit def responseHeadersSemigroupal: Semigroupal[ResponseHeaders] =
    new Semigroupal[ResponseHeaders] {
      def product[A, B](fa: ResponseHeaders[A], fb: ResponseHeaders[B])(implicit
          tupler: Tupler[A, B]
      ): ResponseHeaders[tupler.Out] =
        headers => fa(headers).zip(fb(headers))
    }

  implicit def responseHeadersInvariantFunctor: InvariantFunctor[ResponseHeaders] =
    new InvariantFunctor[ResponseHeaders] {
      def xmap[A, B](
          fa: ResponseHeaders[A],
          f: A => B,
          g: B => A
      ): ResponseHeaders[B] =
        headers => fa(headers).map(f)
    }

  def emptyResponseHeaders: ResponseHeaders[Unit] = _ => Valid(())

  def responseHeader(
      name: String,
      docs: Documentation = None
  ): ResponseHeaders[String] =
    response =>
      Validated.fromOption(Option(response.headers.get(name)))(
        s"Missing response header '$name'"
      )

  def optResponseHeader(
      name: String,
      docs: Documentation = None
  ): ResponseHeaders[Option[String]] =
    response => Valid(Option(response.headers.get(name)))

  def response[A, B, R](
      statusCode: StatusCode,
      entity: ResponseEntity[A],
      docs: Documentation = None,
      headers: ResponseHeaders[B] = emptyResponseHeaders
  )(implicit
      tupler: Tupler.Aux[A, B, R]
  ): Response[R] =
    response =>
      if (response.status == statusCode) {
        headers(response) match {
          case Valid(b) => Some(mapResponseEntity(entity)(tupler(_, b)))
          case Invalid(errors) =>
            Some(_ =>
              js.Promise.resolve[Either[Throwable, R]](Left(new Exception(errors.mkString(". "))))
            )
        }
      } else None

  def choiceResponse[A, B](
      responseA: Response[A],
      responseB: Response[B]
  ): Response[Either[A, B]] =
    response =>
      responseA(response)
        .map(mapResponseEntity(_)(Left(_)))
        .orElse(responseB(response).map(mapResponseEntity(_)(Right(_))))

  abstract class Endpoint[A, B](val request: Request[A], val response: Response[B])
      extends (A => Result[B])

  type Result[A]

  protected final def performFetch[A, B](
      request: Request[A],
      response: Response[B],
      a: A
  )(
      onload: Either[Throwable, B] => Unit,
      onerror: Throwable => Unit
  ): js.Function0[Unit] = {
    val requestData = request(a)
    val requestInit = new FetchRequestInit {}
    requestInit.method = requestData.method
    requestData.prepare(requestInit)
    requestData.entity(requestInit)
    val abortController = new AbortController
    requestInit.signal = abortController.signal

    @volatile var timedOut = false
    val timeoutId = settings.timeout.map { t => scala.scalajs.js.timers.setTimeout(t) { timedOut = true ; abortController.abort() } }

    val f = Fetch.fetch(settings.baseUri.getOrElse("") + request.href(a), requestInit)
    f.`then`(
      (fetchResponse: FetchResponse) => {
        timeoutId.foreach(scala.scalajs.js.timers.clearTimeout)
        val maybeResponse = response(fetchResponse)

        def maybeClientErrors =
          clientErrorsResponse(fetchResponse)
            .map(
              mapPartialResponseEntity[ClientErrors, B](_)(clientErrors =>
                Left(
                  new Exception(
                    clientErrorsToInvalid(clientErrors).errors.mkString(". ")
                  )
                )
              )
            )

        def maybeServerError =
          serverErrorResponse(fetchResponse).map(
            mapPartialResponseEntity[ServerError, B](_)(serverError =>
              Left(serverErrorToThrowable(serverError))
            )
          )

        maybeResponse
          .orElse(maybeClientErrors)
          .orElse(maybeServerError) match {
          case None =>
            onload(Left(new Exception(s"Unexpected response status: ${fetchResponse.status}")))
            Promise.resolve[Unit](()): Unit | js.Thenable[Unit]
          case Some(entityB) =>
            entityB(fetchResponse)
              .`then`(
                (v: Either[Throwable, B]) => onload(v): Unit | js.Thenable[Unit],
                js.defined((e: Any) => {
                  e match {
                    case th: Throwable => onerror(th)
                    case _             => onerror(js.JavaScriptException(e))
                  }
                  (): Unit | js.Thenable[Unit]
                }): js.UndefOr[js.Function1[Any, Unit | js.Thenable[Unit]]]
              ): Unit | js.Thenable[Unit]
        }
      },
      js.defined { (e: Any) =>
        e match {
          case th: Throwable => onerror(th)
          case _             =>
            if(timedOut) onerror(new TimeoutException(s"Server didn't respond in before the request timed out: ${settings.timeout}"))
            else onerror(js.JavaScriptException(e))
        }
        (): Unit | js.Thenable[Unit]
      }: js.UndefOr[js.Function1[Any, Unit | js.Thenable[Unit]]]
    )
    () => abortController.abort()
  }

  override def mapEndpointRequest[A, B, C](
      endpoint: Endpoint[A, B],
      f: Request[A] => Request[C]
  ): Endpoint[C, B] =
    this.endpoint(f(endpoint.request), endpoint.response)

  override def mapEndpointResponse[A, B, C](
      endpoint: Endpoint[A, B],
      f: Response[B] => Response[C]
  ): Endpoint[A, C] =
    this.endpoint(endpoint.request, f(endpoint.response))

  override def mapEndpointDocs[A, B](
      endpoint: Endpoint[A, B],
      f: EndpointDocs => EndpointDocs
  ): Endpoint[A, B] =
    endpoint

  override def addRequestHeaders[A, H](
      request: Request[A],
      headers: RequestHeaders[H]
  )(implicit tupler: Tupler[A, H]): Request[tupler.Out] =
    new Request[tupler.Out] {
      def apply(out: tupler.Out): RequestData = {
        val (a, h) = tupler.unapply(out)
        val requestData = request(a)
        requestData.withPrepare(xhr => {
          requestData.prepare(xhr)
          headers(h, xhr)
        })
      }

      def href(out: tupler.Out): String =
        request.href(tupler.unapply(out)._1)
    }

  override def addRequestQueryString[A, Q](
      request: Request[A],
      qs: QueryString[Q]
  )(implicit tupler: Tupler[A, Q]): Request[tupler.Out] =
    new Request[tupler.Out] {
      def apply(out: tupler.Out): RequestData =
        request(tupler.unapply(out)._1)

      def href(out: tupler.Out): String = {
        val (a, q) = tupler.unapply(out)
        val url = request.href(a)
        qs.encode(q) match {
          case Some(queryString) =>
            if (url.contains('?')) s"$url&$queryString"
            else s"$url?$queryString"
          case None => url
        }
      }
    }

  override def addResponseHeaders[A, H](
      response: Response[A],
      headers: ResponseHeaders[H]
  )(implicit tupler: Tupler[A, H]): Response[tupler.Out] =
    (fetchResponse: FetchResponse) =>
      response(fetchResponse).map[ResponseEntity[tupler.Out]] { a =>
        headers(fetchResponse) match {
          case Valid(h) =>
            mapResponseEntity(a)(tupler(_, h))
          case Invalid(errors) =>
            (_: FetchResponse) =>
              js.Promise.resolve[Either[Throwable, tupler.Out]](
                Left(new Exception(errors.mkString(". ")))
              )
        }
      }
}

final class RequestData private (
    val method: FetchHttpMethod,
    val prepare: js.Function1[FetchRequestInit, Unit],
    val entity: js.Function1[FetchRequestInit, Unit]
) extends Serializable {

  override def toString: String =
    s"RequestData($method, $prepare, $entity)"

  override def equals(obj: Any): Boolean = obj match {
    case other: RequestData =>
      method == other.method && prepare == other.prepare && entity == other.entity
    case _ =>
      false
  }

  override def hashCode(): Int =
    Hashing.hash(method, prepare, entity)

  def withMethod(method: FetchHttpMethod): RequestData =
    copy(method = method)

  def withPrepare(prepare: js.Function1[FetchRequestInit, Unit]): RequestData =
    copy(prepare = prepare)

  def withEntity(entity: js.Function1[FetchRequestInit, Unit]): RequestData =
    copy(entity = entity)

  private def copy(
      method: FetchHttpMethod = method,
      prepare: js.Function1[FetchRequestInit, Unit] = prepare,
      entity: js.Function1[FetchRequestInit, Unit] = entity
  ): RequestData = new RequestData(method, prepare, entity)

}

object RequestData {

  def apply(
      method: FetchHttpMethod,
      prepare: js.Function1[FetchRequestInit, Unit],
      entity: js.Function1[FetchRequestInit, Unit]
  ): RequestData =
    new RequestData(method, prepare, entity)
}
