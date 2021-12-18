package endpoints4s.xhr.future

import endpoints4s.xhr.FutureLike

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.scalajs.js

trait FutureBasedFutureLike extends FutureLike {
  implicit val ec: ExecutionContext

  override type FutureLike[A] = Future[A]

  override def futureLike[A](
      creator: js.Function2[js.Function1[A, _], js.Function1[Throwable, _], _]
  ): Future[A] = {
    val promise = Promise[A]()
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
    futureLike.flatMap(f)
  }

  override def onCompleteFutureLike[A, B](
      futureLike: Future[A],
      success: js.Function1[A, B],
      failure: js.Function1[Throwable, B]
  ): Unit = {
    futureLike.onComplete(_.fold(failure, success))
  }
}
