package julienrf.endpoints

import cats.data.Xor
import io.circe.{Decoder, Encoder, parse}
import org.scalajs.dom.raw.{XMLHttpRequest, Promise}

import scala.scalajs.js

trait XhrClient extends Endpoints {

  type Path[A] = js.Function1[A, String]

  def static(segment: String) = _ => segment

  def dynamic: js.Function1[String, String] = (s: String) => scalajs.js.URIUtils.encodeURIComponent(s)

  def chained[A, B](first: Path[A], second: Path[B])(implicit fc: FlatConcat[A, B]): Path[fc.Out] =
    (out: fc.Out) => {
      val (a, b) = fc.unapply(out)
      first(a) ++ "/" ++ second(b)
    }


  type Request[A] = js.Function1[A, XMLHttpRequest]

  def get[A](path: Path[A]) =
    a => {
      val xhr = new XMLHttpRequest
      xhr.open("GET", "/" ++ path(a))
      xhr
    }


  type Response[A] = js.Function1[XMLHttpRequest, Xor[Exception, A]]

  def jsonEntity[A](implicit decoder: Decoder[A]): js.Function1[XMLHttpRequest, Xor[Exception, A]] =
    xhr => parse.parse(xhr.responseText).flatMap(decoder.decodeJson)


  type Endpoint[A, B] = js.Function1[A, Promise[B]]
  type RequestMarshaller[A] = Encoder[A]
  type ResponseMarshaller[A] = Decoder[A]

  def endpoint[A, B](request: Request[A], response: Response[B]): Endpoint[A, B] =
    (a: A) =>
      new Promise[B]((resolve, error) => {
        val xhr = request(a)
        xhr.onload = _ => response(xhr).fold(exn => error(exn.getMessage), b => resolve(b))
        xhr.onerror = _ => error(xhr.responseText)
        xhr.send()
      })

}
