package endpoints

import cats.data.Xor
import org.scalajs.dom.XMLHttpRequest

import scala.scalajs.js

trait XhrClient extends EndpointsAlg {

  class Path[A](val apply: js.Function1[A, String]) extends PathOps[A]

  def staticPathSegment(segment: String) = new Path(_ => segment)

  def segment[A](implicit s: Segment[A]): Path[A] =
    new Path(s)

  def chainPaths[A, B](first: Path[A], second: Path[B])(implicit fc: FlatConcat[A, B]): Path[fc.Out] =
    new Path((out: fc.Out) => {
      val (a, b) = fc.unapply(out)
      first.apply(a) ++ "/" ++ second.apply(b)
    })

  type Segment[A] = js.Function1[A, String]

  implicit def stringSegment: Segment[String] =
    (s: String) => js.URIUtils.encodeURIComponent(s)

  implicit def intSegment: Segment[Int] =
    (i: Int) => i.toString


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

  type Response[A] = js.Function1[XMLHttpRequest, Xor[Exception, A]]

  val emptyResponse: Response[Unit] = _ => Xor.Right(())


  type Endpoint[A, B] = js.Function1[A, js.Promise[B]]

  def endpoint[A, B](request: Request[A], response: Response[B]): Endpoint[A, B] =
    (a: A) =>
      new js.Promise[B]((resolve, error) => {
        val (xhr, maybeEntity) = request(a)
        xhr.onload = _ => response(xhr).fold(exn => error(exn.getMessage), b => resolve(b))
        xhr.onerror = _ => error(xhr.responseText)
        xhr.send(maybeEntity.orNull)
      })

}
