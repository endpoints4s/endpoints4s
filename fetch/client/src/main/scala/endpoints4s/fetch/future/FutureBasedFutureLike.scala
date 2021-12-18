package endpoints4s.fetch.future

import endpoints4s.fetch.FutureLike

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
}
