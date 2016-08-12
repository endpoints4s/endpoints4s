package endpoints

import org.scalajs.dom.XMLHttpRequest

import scala.language.higherKinds
import scala.scalajs.js

trait EndpointXhrClient extends EndpointAlg with UrlClient {

  type Headers[A] = js.Function2[A, XMLHttpRequest, Unit]

  lazy val emptyHeaders: Headers[Unit] = (_, _) => ()


  type Request[A] = js.Function1[A, (XMLHttpRequest, Option[js.Any])]

  type RequestEntity[A] = js.Function2[A, XMLHttpRequest, String /* TODO String | Blob | FormData | … */]

  def get[A, B](url: Url[A], headers: Headers[B])(implicit tupler: Tupler[A, B]): Request[tupler.Out] =
    (ab: tupler.Out) => {
      val (a, b) = tupler.unapply(ab)
      val xhr = makeXhr("GET", url, a, headers, b)
      (xhr, None)
    }

  def post[A, B, C, AB](url: Url[A], entity: RequestEntity[B], headers: Headers[C])(implicit tuplerAB: Tupler.Aux[A, B, AB], tuplerABC: Tupler[AB, C]): Request[tuplerABC.Out] =
    (abc: tuplerABC.Out) => {
      val (ab, c) = tuplerABC.unapply(abc)
      val (a, b) = tuplerAB.unapply(ab)
      val xhr = makeXhr("POST", url, a, headers, c)
      (xhr, Some(entity(b, xhr)))
    }

  private def makeXhr[A, B](method: String, url: Url[A], a: A, headers: Headers[B], b: B): XMLHttpRequest = {
    val xhr = new XMLHttpRequest
    xhr.open(method, url.encode(a))
    headers(b, xhr)
    xhr
  }

  type Response[A] = js.Function1[XMLHttpRequest, Either[Exception, A]]

  lazy val emptyResponse: Response[Unit] = _ => Right(())


  type Task[A]

  type Endpoint[A, B] = js.Function1[A, Task[B]]

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
