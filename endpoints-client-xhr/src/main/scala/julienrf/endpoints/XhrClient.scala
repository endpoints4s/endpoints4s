package julienrf.endpoints

import cats.data.Xor
import io.circe.{Decoder, Encoder, parse}
import org.scalajs.dom.raw.{XMLHttpRequest, Promise}

import scala.scalajs.js

trait XhrClient extends Endpoints {

  class Path[A](val apply: js.Function1[A, String]) extends PathOps[A]

  def static(segment: String) = new Path(_ => segment)

  def dynamic = new Path((s: String) => scalajs.js.URIUtils.encodeURIComponent(s))

  def chained[A, B](first: Path[A], second: Path[B])(implicit fc: FlatConcat[A, B]): Path[fc.Out] =
    new Path((out: fc.Out) => {
      val (a, b) = fc.unapply(out)
      first.apply(a) ++ "/" ++ second.apply(b)
    })


  type Request[A] = js.Function1[A, (XMLHttpRequest, Option[js.Any])]

  type RequestEntity[A] = js.Function2[A, XMLHttpRequest, String /* TODO String | Blob | FormData | … */]

  def get[A](path: Path[A]) =
    a => {
      val xhr = new XMLHttpRequest
      xhr.open("GET", path.apply(a))
      (xhr, None)
    }

  def post[A, B](path: Path[A], entity: RequestEntity[B])(implicit fc: FlatConcat[A, B]): Request[fc.Out] =
    (out: fc.Out) => {
      val (a, b) = fc.unapply(out)
      val xhr = new XMLHttpRequest
      xhr.open("POST", path.apply(a))
      (xhr, Some(entity(b, xhr)))
    }

  def jsonRequest[A : JsonRequest] = (a: A, xhr: XMLHttpRequest) => {
    xhr.setRequestHeader("Content-Type", "application/json")
    Encoder[A].apply(a).noSpaces
  }

  type Response[A] = js.Function1[XMLHttpRequest, Xor[Exception, A]]

  def jsonResponse[A](implicit decoder: Decoder[A]): js.Function1[XMLHttpRequest, Xor[Exception, A]] =
    xhr => parse.parse(xhr.responseText).flatMap(decoder.decodeJson)


  type Endpoint[A, B] = js.Function1[A, Promise[B]]
  type JsonRequest[A] = Encoder[A]
  type JsonResponse[A] = Decoder[A]

  def endpoint[A, B](request: Request[A], response: Response[B]): Endpoint[A, B] =
    (a: A) =>
      new Promise[B]((resolve, error) => {
        val (xhr, maybeEntity) = request(a)
        xhr.onload = _ => response(xhr).fold(exn => error(exn.getMessage), b => resolve(b))
        xhr.onerror = _ => error(xhr.responseText)
        xhr.send(maybeEntity.orNull)
      })

}
