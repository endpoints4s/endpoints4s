package endpoints

import scala.scalajs.js

trait ThenableClient extends XhrClient {

  type Task[A] = js.Thenable[A]

  def endpoint[A, B](request: Request[A], response: Response[B]): Endpoint[A, B] =
    (a: A) =>
      new js.Promise[B]((resolve, error) => {
        performXhr(request, response, a)(
          _.fold(exn => error(exn.getMessage), b => resolve(b)),
          xhr => error(xhr.responseText)
        )
      })

}
