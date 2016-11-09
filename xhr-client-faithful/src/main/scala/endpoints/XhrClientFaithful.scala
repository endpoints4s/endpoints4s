package endpoints

import faithful.{Future, Promise}

/**
  * Implements [[EndpointXhrClient]] by using faithful.
  */
trait XhrClientFaithful extends EndpointXhrClient {

  /** Maps `Task` to [[Future]] */
  type Task[A] = Future[A]

  def endpoint[A, B](request: Request[A], response: Response[B]) =
    (a: A) => {
      val promise = new Promise[B]()
      performXhr(request, response, a)(
        _.fold(promise.failure, promise.success),
        xhr => promise.failure(new Exception(xhr.responseText))
      )
      promise.future
    }

}