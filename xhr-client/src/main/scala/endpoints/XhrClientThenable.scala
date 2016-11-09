package endpoints

import scala.scalajs.js

/**
  * Implements [[EndpointXhrClient]] by using JavaScript promises.
  */
trait XhrClientThenable extends EndpointXhrClient {

  /** Maps a `Task` to a [[js.Thenable]] */
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
