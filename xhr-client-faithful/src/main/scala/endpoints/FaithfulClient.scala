package endpoints

import faithful.{Future, Promise}

trait FaithfulClient extends XhrClient {

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