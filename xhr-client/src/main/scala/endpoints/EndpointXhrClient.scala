package endpoints

import org.scalajs.dom.XMLHttpRequest

import scala.language.higherKinds
import scala.scalajs.js

/**
  * Interpreter for [[EndpointAlg]] that builds a client issuing requests
  * using XMLHttpRequest.
  */
trait EndpointXhrClient extends EndpointAlg with UrlClient {

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
    * a request entity (as a String).
    */
  type RequestEntity[A] = js.Function2[A, XMLHttpRequest, String]

  def get[A, B](url: Url[A], headers: RequestHeaders[B])(implicit tupler: Tupler[A, B]): Request[tupler.Out] =
    (ab: tupler.Out) => {
      val (a, b) = tupler.unapply(ab)
      val xhr = makeXhr("GET", url, a, headers, b)
      (xhr, None)
    }

  def post[A, B, C, AB](url: Url[A], entity: RequestEntity[B], headers: RequestHeaders[C])(implicit tuplerAB: Tupler.Aux[A, B, AB], tuplerABC: Tupler[AB, C]): Request[tuplerABC.Out] =
    (abc: tuplerABC.Out) => {
      val (ab, c) = tuplerABC.unapply(abc)
      val (a, b) = tuplerAB.unapply(ab)
      val xhr = makeXhr("POST", url, a, headers, c)
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
    * A function that takes the information needed to build a request and returns
    * a task yielding the information carried by the response.
    *
    * @tparam A Information carried by the request
    * @tparam B Information carried by the response
    */
  type Endpoint[A, B] = js.Function1[A, Task[B]]

  /**
    * A task that eventually yields an `A`.
    *
    * Typically, concrete representation of `Task` will have an instance of `MonadError`, so
    * that we can perform requests (sequentially and in parallel) and recover errors.
    */
  type Task[A]

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

}
