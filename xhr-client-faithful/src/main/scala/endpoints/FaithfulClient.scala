package endpoints

import faithful.{Future, Promise}

trait FaithfulClient extends XhrClient {

  type Task[A] = Future[A]

  def endpoint[A, B](request: Request[A], response: Response[B]) =
    (a: A) => {
      val promise = new Promise[B]()
      val (xhr, maybeEntity) = request(a)
      xhr.onload = _ => response(xhr).fold(exn => promise.failure(new Exception(exn.getMessage)), promise.success)
      xhr.onerror = _ => promise.failure(new Exception(xhr.responseText))
      xhr.send(maybeEntity.orNull)
      promise.future
    }

}