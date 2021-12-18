package endpoints4s.xhr.faithful

import endpoints4s.xhr.FutureLike
import faithful.Future
import faithful.Promise

import scala.scalajs.js

trait FaithfulBasedFutureLike extends FutureLike {

  type FutureLike[A] = Future[A]

  override def futureLike[A](
      creator: js.Function2[js.Function1[A, _], js.Function1[Throwable, _], _]
  ): Future[A] = {
    val promise = new Promise[A]()
    creator(
      a => promise.success(a),
      promise.failure _
    )
    promise.future
  }

  override def flatMapFutureLike[A, B](
      futureLike: Future[A],
      f: js.Function1[A, Future[B]]
  ): Future[B] = {
    val promise = new Promise[B]()
    futureLike(a => f(a)(b => promise.success(b), e => promise.failure(e)), e => promise.failure(e))
    promise.future
  }

  override def onCompleteFutureLike[A, B](
      futureLike: Future[A],
      success: js.Function1[A, B],
      failure: js.Function1[Throwable, B]
  ): Unit = {
    futureLike(a => success(a), e => failure(e))
    ()
  }
}
