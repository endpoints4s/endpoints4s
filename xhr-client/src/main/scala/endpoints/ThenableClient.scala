package endpoints

import scala.scalajs.js

trait ThenableClient extends XhrClient {

  type Task[A] = js.Thenable[A]

  def endpoint[A, B](request: Request[A], response: Response[B]): Endpoint[A, B] =
    (a: A) =>
      new js.Promise[B]((resolve, error) => {
        val (xhr, maybeEntity) = request(a)
        xhr.onload = _ => response(xhr).fold(exn => error(exn.getMessage), b => resolve(b))
        xhr.onerror = _ => error(xhr.responseText)
        xhr.send(maybeEntity.orNull)
      })

}
