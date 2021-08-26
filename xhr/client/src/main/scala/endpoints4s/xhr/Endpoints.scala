package endpoints4s.xhr

import endpoints4s.{
  Decoder,
  Hashing,
  Invalid,
  InvariantFunctor,
  PartialInvariantFunctor,
  Semigroupal,
  Tupler,
  Valid,
  Validated,
  algebra
}
import endpoints4s.algebra.Documentation
import org.scalajs.dom.XMLHttpRequest

import scala.scalajs.js

/** Partial interpreter for [[algebra.Endpoints]] that builds a client issuing requests
  * using XMLHttpRequest. It uses [[algebra.BuiltInErrors]] to model client and server errors.
  *
  * The interpreter is ''partially'' implemented: it returns endpoint invocation
  * results in an abstract `Result` type, which is yet to be defined
  * by a more specialized interpreter. You can find such interpreters
  * in the “known `Endpoints` subclasses” list.
  *
  * @group interpreters
  */
trait Endpoints extends algebra.Endpoints with EndpointsWithCustomErrors with BuiltInErrors

/** Partial interpreter for [[algebra.Endpoints]] that builds a client issuing requests
  * using XMLHttpRequest.
  *
  * @group interpreters
  */
trait EndpointsWithCustomErrors
    extends algebra.EndpointsWithCustomErrors
    with Urls
    with Methods
    with StatusCodes {

  /** A function that takes the information `A` and the XMLHttpRequest
    * and sets up some headers on it.
    */
  type RequestHeaders[A] = js.Function2[A, XMLHttpRequest, Unit]

  /** Sets up no headers on the given XMLHttpRequest */
  lazy val emptyRequestHeaders: RequestHeaders[Unit] = (_, _) => ()

  def requestHeader(
      name: String,
      docs: endpoints4s.algebra.Documentation
  ): RequestHeaders[String] =
    (value, xhr) => xhr.setRequestHeader(name, value)

  def optRequestHeader(
      name: String,
      docs: endpoints4s.algebra.Documentation
  ): RequestHeaders[Option[String]] =
    (valueOpt, xhr) => valueOpt.foreach(value => xhr.setRequestHeader(name, value))

  implicit lazy val requestHeadersPartialInvariantFunctor: PartialInvariantFunctor[RequestHeaders] =
    new PartialInvariantFunctor[RequestHeaders] {
      override def xmapPartial[From, To](
          f: js.Function2[From, XMLHttpRequest, Unit],
          map: From => Validated[To],
          contramap: To => From
      ): js.Function2[To, XMLHttpRequest, Unit] =
        (to, xhr) => f(contramap(to), xhr)
    }

  implicit lazy val requestHeadersSemigroupal: Semigroupal[RequestHeaders] =
    new Semigroupal[RequestHeaders] {
      override def product[A, B](
          fa: js.Function2[A, XMLHttpRequest, Unit],
          fb: js.Function2[B, XMLHttpRequest, Unit]
      )(implicit
          tupler: Tupler[A, B]
      ): js.Function2[tupler.Out, XMLHttpRequest, Unit] =
        (out, xhr) => {
          val (a, b) = tupler.unapply(out)
          fa(a, xhr)
          fb(b, xhr)
        }
    }

  /** A function that takes the information `A` and returns the data to create
    * an XMLHttpRequest.
    */
  // FIXME Use a representation that makes it easier to set the request Content-Type header according to its entity type
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

  /** A function that, given information `A` and an XMLHttpRequest, returns
    * a request entity.
    * Also, as a side-effect, the function can set the corresponding Content-Type header
    * on the given XMLHttpRequest.
    */
  type RequestEntity[A] = js.Function2[A, XMLHttpRequest, js.Any]

  lazy val emptyRequest: RequestEntity[Unit] = (_, _) => null

  lazy val textRequest: RequestEntity[String] = (body, xhr) => {
    xhr.setRequestHeader("Content-type", "text/plain; charset=utf8")
    body
  }

  def choiceRequestEntity[A, B](
      requestEntityA: RequestEntity[A],
      requestEntityB: RequestEntity[B]
  ): RequestEntity[Either[A, B]] =
    (eitherAB, xhr) => eitherAB.fold(requestEntityA(_, xhr), requestEntityB(_, xhr))

  implicit lazy val requestEntityPartialInvariantFunctor: PartialInvariantFunctor[RequestEntity] =
    new PartialInvariantFunctor[RequestEntity] {
      def xmapPartial[From, To](
          f: js.Function2[From, XMLHttpRequest, js.Any],
          map: From => Validated[To],
          contramap: To => From
      ): js.Function2[To, XMLHttpRequest, js.Any] =
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
        val (a, b) = tuplerAB.unapply(ab)
        RequestData(method, headers(c, _), xhr => Some(entity(b, xhr)))
      }

      def href(abc: Out) = {
        val (ab, _) = tuplerABC.unapply(abc)
        val (a, _) = tuplerAB.unapply(ab)
        url.encode(a)
      }
    }

  /** Attempts to decode an `A` from an XMLHttpRequest’s response
    */
  type Response[A] = js.Function1[XMLHttpRequest, Option[ResponseEntity[A]]]

  implicit lazy val responseInvariantFunctor: InvariantFunctor[Response] =
    new InvariantFunctor[Response] {
      def xmap[A, B](fa: Response[A], f: A => B, g: B => A): Response[B] =
        xhr => fa(xhr).map(mapResponseEntity(_)(f))
    }

  type ResponseEntity[A] = js.Function1[XMLHttpRequest, Either[Throwable, A]]

  implicit def responseEntityInvariantFunctor: InvariantFunctor[ResponseEntity] =
    new InvariantFunctor[ResponseEntity] {
      def xmap[A, B](
          fa: ResponseEntity[A],
          f: A => B,
          g: B => A
      ): ResponseEntity[B] =
        mapResponseEntity(fa)(f)
    }

  private[xhr] def mapResponseEntity[A, B](
      entity: ResponseEntity[A]
  )(f: A => B): ResponseEntity[B] =
    mapPartialResponseEntity(entity)(a => Right(f(a)))

  private[xhr] def mapPartialResponseEntity[A, B](
      entity: ResponseEntity[A]
  )(f: A => Either[Throwable, B]): ResponseEntity[B] =
    xhr => entity(xhr).flatMap(f)

  def stringCodecResponse[A](implicit
      codec: Decoder[String, A]
  ): ResponseEntity[A] =
    xhr =>
      codec
        .decode(xhr.responseText)
        .fold(Right(_), errors => Left(new Exception(errors.mkString(". "))))

  /** Discards response entity
    */
  def emptyResponse: ResponseEntity[Unit] = _ => Right(())

  /** Decodes a text entity
    */
  def textResponse: ResponseEntity[String] = xhr => Right(xhr.responseText)

  type ResponseHeaders[A] = XMLHttpRequest => Validated[A]

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
    xhr =>
      Validated.fromOption(Option(xhr.getResponseHeader(name)))(
        s"Missing response header '$name'"
      )

  def optResponseHeader(
      name: String,
      docs: Documentation = None
  ): ResponseHeaders[Option[String]] =
    xhr => Valid(Option(xhr.getResponseHeader(name)))

  def response[A, B, R](
      statusCode: StatusCode,
      entity: ResponseEntity[A],
      docs: Documentation = None,
      headers: ResponseHeaders[B] = emptyResponseHeaders
  )(implicit
      tupler: Tupler.Aux[A, B, R]
  ): Response[R] =
    xhr =>
      if (xhr.status == statusCode) {
        headers(xhr) match {
          case Valid(b) => Some(mapResponseEntity(entity)(tupler(_, b)))
          case Invalid(errors) =>
            Some(_ => Left(new Exception(errors.mkString(". "))))
        }
      } else None

  def choiceResponse[A, B](
      responseA: Response[A],
      responseB: Response[B]
  ): Response[Either[A, B]] =
    xhr =>
      responseA(xhr)
        .map(mapResponseEntity(_)(Left(_)))
        .orElse(responseB(xhr).map(mapResponseEntity(_)(Right(_))))

  /** A function that takes the information needed to build a request and returns
    * a task yielding the information carried by the response.
    */
  abstract class Endpoint[A, B](val request: Request[A], val response: Response[B])
      extends (A => Result[B]) {
    def href(a: A): String = request.href(a)
  }

  /** A value that eventually yields an `A`.
    *
    * Typically, concrete representation of `Result` will have an instance of `MonadError`, so
    * that we can perform requests (sequentially and in parallel) and recover errors.
    */
  type Result[A]

  protected final def performXhr[A, B](
      request: Request[A],
      response: Response[B],
      a: A
  )(
      onload: Either[Throwable, B] => Unit,
      onerror: XMLHttpRequest => Unit
  ): Unit = {
    val requestData = request(a)
    val xhr = new XMLHttpRequest
    xhr.open(requestData.method, request.href(a))
    requestData.prepare(xhr)
    val maybeEntity = requestData.entity(xhr)

    xhr.onload = _ => {
      val maybeResponse = response(xhr)
      def maybeClientErrors =
        clientErrorsResponse(xhr)
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
        serverErrorResponse(xhr).map(
          mapPartialResponseEntity[ServerError, B](_)(serverError =>
            Left(serverErrorToThrowable(serverError))
          )
        )
      val maybeB =
        maybeResponse
          .orElse(maybeClientErrors)
          .orElse(maybeServerError)
          .toRight(new Exception(s"Unexpected response status: ${xhr.status}"))
          .flatMap(_(xhr))
      onload(maybeB)
    }
    xhr.onerror = _ => onerror(xhr)
    xhr.send(maybeEntity.orNull)
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
    (xhr: XMLHttpRequest) =>
      response(xhr).map[ResponseEntity[tupler.Out]] { a =>
        headers(xhr) match {
          case Valid(h) =>
            mapResponseEntity(a)(tupler(_, h))
          case Invalid(errors) =>
            (_: XMLHttpRequest) => Left(new Exception(errors.mkString(". ")))
        }
      }
}

final class RequestData private (
    val method: String,
    val prepare: js.Function1[XMLHttpRequest, Unit],
    val entity: js.Function1[XMLHttpRequest, Option[js.Any]]
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

  def withMethod(method: String): RequestData =
    copy(method = method)

  def withPrepare(prepare: js.Function1[XMLHttpRequest, Unit]): RequestData =
    copy(prepare = prepare)

  def withEntity(entity: js.Function1[XMLHttpRequest, Option[js.Any]]): RequestData =
    copy(entity = entity)

  private def copy(
      method: String = method,
      prepare: js.Function1[XMLHttpRequest, Unit] = prepare,
      entity: js.Function1[XMLHttpRequest, Option[js.Any]] = entity
  ): RequestData = new RequestData(method, prepare, entity)

}

object RequestData {

  def apply(
      method: String,
      prepare: js.Function1[XMLHttpRequest, Unit],
      entity: js.Function1[XMLHttpRequest, Option[js.Any]]
  ): RequestData =
    new RequestData(method, prepare, entity)
}
