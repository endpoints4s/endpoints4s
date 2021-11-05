package endpoints4s.fetch

import endpoints4s.Decoder
import endpoints4s.Invalid
import endpoints4s.InvariantFunctor
import endpoints4s.PartialInvariantFunctor
import endpoints4s.Semigroupal
import endpoints4s.Tupler
import endpoints4s.Valid
import endpoints4s.Validated
import endpoints4s.algebra
import endpoints4s.algebra.Documentation
import org.scalajs.dom.experimental.Fetch
import org.scalajs.dom.experimental.{Headers => FetchHeaders}
import org.scalajs.dom.experimental.{RequestInit => FetchRequestInit}
import org.scalajs.dom.experimental.{Response => FetchResponse}

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.Thenable.Implicits._
import scala.util.Try

trait Endpoints extends algebra.Endpoints with EndpointsWithCustomErrors with BuiltInErrors

trait EndpointsWithCustomErrors
    extends algebra.EndpointsWithCustomErrors
    with Urls
    with Methods
    with StatusCodes {

  def endpointsSettings: EndpointsSettings
  implicit def ec: ExecutionContext

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

  // Future is returned base RequestEntity needs it
  type Request[A] = A => Future[(FetchUrl, FetchRequestInit)]

  implicit def requestPartialInvariantFunctor: PartialInvariantFunctor[Request] =
    new PartialInvariantFunctor[Request] {
      def xmapPartial[A, B](
          fa: Request[A],
          f: A => Validated[B],
          g: B => A
      ): Request[B] =
        (b: B) => fa(g(b))
    }

  // Returning Future is required only because of potential need to work with Promises in chunked requests
  type RequestEntity[A] = (A, FetchRequestInit) => Future[Unit]

  lazy val emptyRequest: RequestEntity[Unit] = (_, _) => Future.unit

  lazy val textRequest: RequestEntity[String] = (body, requestInit) =>
    Future.fromTry(Try {
      requestInit.setRequestHeader("Content-type", "text/plain; charset=utf8")
      requestInit.body = body
    })

  def choiceRequestEntity[A, B](
      requestEntityA: RequestEntity[A],
      requestEntityB: RequestEntity[B]
  ): RequestEntity[Either[A, B]] =
    (eitherAB, requestInit) =>
      eitherAB.fold(requestEntityA(_, requestInit), requestEntityB(_, requestInit))

  implicit lazy val requestEntityPartialInvariantFunctor: PartialInvariantFunctor[RequestEntity] =
    new PartialInvariantFunctor[RequestEntity] {
      def xmapPartial[From, To](
          f: (From, FetchRequestInit) => Future[Unit],
          map: From => Validated[To],
          contramap: To => From
      ): (To, FetchRequestInit) => Future[Unit] =
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
    (abc: Out) => {
      val (ab, c) = tuplerABC.unapply(abc)
      val (a, b) = tuplerAB.unapply(ab)
      val (fetchUrl, requestInit) = makeFetch(method, url, a, headers, c)
      entity(b, requestInit)
        .map(_ => (fetchUrl, requestInit))
    }

  private def makeFetch[A, B](
      method: Method,
      url: Url[A],
      a: A,
      headers: RequestHeaders[B],
      b: B
  ) = {
    val requestInit = new FetchRequestInit {}
    requestInit.method = method
    headers(b, requestInit)
    (FetchUrl(url.encode(a)), requestInit)
  }

  type Response[A] = js.Function1[FetchResponse, Option[ResponseEntity[A]]]

  implicit lazy val responseInvariantFunctor: InvariantFunctor[Response] =
    new InvariantFunctor[Response] {
      def xmap[A, B](fa: Response[A], f: A => B, g: B => A): Response[B] =
        xhr => fa(xhr).map(mapResponseEntity(_)(f))
    }

  type ResponseEntity[A] = FetchResponse => Future[Either[Throwable, A]]

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
    response => entity(response).map(_.flatMap(f))

  def stringCodecResponse[A](implicit
      codec: Decoder[String, A]
  ): ResponseEntity[A] = _.text().map(text =>
    codec
      .decode(text)
      .fold(Right(_), errors => Left(new Exception(errors.mkString(". "))))
  )

  def emptyResponse: ResponseEntity[Unit] = _ => Future.successful(Right(()))

  def textResponse: ResponseEntity[String] = response => response.text().map(Right(_))

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
            Some(_ => Future.successful(Left(new Exception(errors.mkString(". ")))))
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

  type Endpoint[A, B] = A => Result[B]

  type Result[A]

  protected final def performFetch[A, B](
      request: Request[A],
      response: Response[B],
      a: A
  )(
      onload: Either[Throwable, B] => Unit,
      onerror: Throwable => Unit
  ): Unit = {
    request(a)
      .foreach { case (url, requestInit) =>
        val f = Fetch
          .fetch(endpointsSettings.host.getOrElse("") + url.underlying, requestInit)
        f.foreach { fetchResponse =>
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
            case Some(entityB) =>
              entityB(fetchResponse).foreach(onload)
          }
        }
        f.failed.foreach(onerror)
      }
  }
}
