package endpoints.xhr

import endpoints.{InvariantFunctor, Semigroupal, Tupler, algebra}
import endpoints.algebra.Documentation
import org.scalajs.dom.XMLHttpRequest

import scala.language.higherKinds
import scala.scalajs.js

/**
  * Partial interpreter for [[algebra.Endpoints]] that builds a client issuing requests
  * using XMLHttpRequest.
  *
  * The interpreter is ''partially'' implemented: it returns endpoint invocation
  * results in an abstract `Result` type, which is yet to be defined
  * by a more specialized interpreter. You can find such interpreters
  * in the “known `Endpoints` subclasses” list.
  *
  * @group interpreters
  */
trait Endpoints extends algebra.Endpoints with Urls with Methods with StatusCodes{

  /**
    * A function that takes the information `A` and the XMLHttpRequest
    * and sets up some headers on it.
    */
  type RequestHeaders[A] = js.Function2[A, XMLHttpRequest, Unit]

  /** Sets up no headers on the given XMLHttpRequest */
  lazy val emptyHeaders: RequestHeaders[Unit] = (_, _) => ()

  def header(name: String, docs: endpoints.algebra.Documentation): RequestHeaders[String] =
    (value, xhr) => xhr.setRequestHeader(name, value)

  def optHeader(name: String, docs: endpoints.algebra.Documentation): RequestHeaders[Option[String]] =
    (valueOpt, xhr) => valueOpt.foreach(value => xhr.setRequestHeader(name, value))

  implicit lazy val reqHeadersInvFunctor: InvariantFunctor[RequestHeaders] = new InvariantFunctor[RequestHeaders] {
    override def xmap[From, To](f: js.Function2[From, XMLHttpRequest, Unit], map: From => To, contramap: To => From): js.Function2[To, XMLHttpRequest, Unit] =
      (to, xhr) => f(contramap(to), xhr)
  }

  implicit lazy val reqHeadersSemigroupal: Semigroupal[RequestHeaders] = new Semigroupal[RequestHeaders]{
    override def product[A, B](fa: js.Function2[A, XMLHttpRequest, Unit], fb: js.Function2[B, XMLHttpRequest, Unit])(implicit tupler: Tupler[A, B]): js.Function2[tupler.Out, XMLHttpRequest, Unit] =
      (out, xhr) => {
        val (a, b) = tupler.unapply(out)
        fa(a, xhr)
        fb(b, xhr)
      }
  }

  /**
    * A function that takes the information `A` and returns an XMLHttpRequest
    * with an optional request entity. If provided, the request entity must be
    * compatible with the `send` method of XMLHttpRequest.
    */
  // FIXME Use a representation that makes it easier to set the request Content-Type header according to its entity type
  trait Request[A] {
    def apply(a: A): (XMLHttpRequest, Option[js.Any])

    def href(a: A): String
  }

  /**
    * A function that, given information `A` and an XMLHttpRequest, returns
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

  implicit lazy val reqEntityInvFunctor: InvariantFunctor[RequestEntity] = new InvariantFunctor[RequestEntity] {
    override def xmap[From, To](f: js.Function2[From, XMLHttpRequest, js.Any], map: From => To, contramap: To => From): js.Function2[To, XMLHttpRequest, js.Any] =
      (to, xhr) => f(contramap(to), xhr)
  }

  def request[A, B, C, AB, Out](
    method: Method,
    url: Url[A],
    entity: RequestEntity[B],
    docs: Documentation,
    headers: RequestHeaders[C]
  )(implicit tuplerAB: Tupler.Aux[A, B, AB], tuplerABC: Tupler.Aux[AB, C, Out]): Request[Out] =
    new Request[Out] {
      def apply(abc: Out) = {
        val (ab, c) = tuplerABC.unapply(abc)
        val (a, b) = tuplerAB.unapply(ab)
        val xhr = makeXhr(method, url, a, headers, c)
        (xhr, Some(entity(b, xhr)))
      }

      def href(abc: Out) = {
        val (ab, _) = tuplerABC.unapply(abc)
        val (a, _) = tuplerAB.unapply(ab)
        url.encode(a)
      }
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
  type Response[A] = js.Function1[XMLHttpRequest, Option[ResponseEntity[A]]]

  implicit lazy val responseInvFunctor: InvariantFunctor[Response] =
    new InvariantFunctor[Response] {
      def xmap[A, B](fa: Response[A], f: A => B, g: B => A): Response[B] =
        xhr => fa(xhr).map(mapResponseEntity(_)(f))
    }

  type ResponseEntity[A] = js.Function1[XMLHttpRequest, Either[Exception, A]]

  private[xhr] def mapResponseEntity[A, B](entity: ResponseEntity[A])(f: A => B): ResponseEntity[B] =
    xhr => entity(xhr).right.map(f)

  /**
    * Discards response entity
    */
  def emptyResponse: ResponseEntity[Unit] = _ => Right(())

  /**
    * Decodes a text entity
    */
  def textResponse: ResponseEntity[String] = xhr => Right(xhr.responseText)

  def response[A](statusCode: StatusCode, entity: ResponseEntity[A], docs: Documentation = None): Response[A] =
    xhr =>
      if (xhr.status == statusCode) Some(entity)
      else None

  def choiceResponse[A, B](responseA: Response[A], responseB: Response[B]): Response[Either[A, B]] =
    xhr =>
      responseA(xhr).map(mapResponseEntity(_)(Left(_)))
        .orElse(responseB(xhr).map(mapResponseEntity(_)(Right(_))))

  /**
    * A function that takes the information needed to build a request and returns
    * a task yielding the information carried by the response.
    */
  abstract class Endpoint[A, B](request: Request[A]) {
    def apply(a: A): Result[B]

    def href(a: A): String = request.href(a)
  }

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
    xhr.onload = _ => onload(response(xhr).toRight(new Exception(s"Unexpected response status: ${xhr.status}")).right.flatMap(_(xhr)))
    xhr.onerror = _ => onerror(xhr)
    xhr.send(maybeEntity.orNull)
  }

}
