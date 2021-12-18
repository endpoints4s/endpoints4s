package endpoints4s.xhr

import scala.scalajs.js

trait FutureLike {

  type FutureLike[A]

  def futureLike[A](
      creator: js.Function2[js.Function1[A, _], js.Function1[Throwable, _], _]
  ): FutureLike[A]

  def flatMapFutureLike[A, B](
      futureLike: FutureLike[A],
      f: js.Function1[A, FutureLike[B]]
  ): FutureLike[B]

  def onCompleteFutureLike[A, B](
      futureLike: FutureLike[A],
      success: js.Function1[A, B],
      failure: js.Function1[Throwable, B]
  ): Unit

  implicit class FutureLikeOps[A](futureLike: FutureLike[A]) {
    def flatMap[B](f: js.Function1[A, FutureLike[B]]): FutureLike[B] =
      flatMapFutureLike(futureLike, f)

    def onComplete[B](
        success: js.Function1[A, B],
        failure: js.Function1[Throwable, B]
    ): Unit = onCompleteFutureLike(futureLike, success, failure)
  }
}
