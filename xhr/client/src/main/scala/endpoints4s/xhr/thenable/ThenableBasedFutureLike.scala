package endpoints4s.xhr.thenable

import endpoints4s.xhr.FutureLike

import scala.scalajs.js
import scala.scalajs.js.|

trait ThenableBasedFutureLike extends FutureLike {
  override type FutureLike[A] = js.Thenable[A]

  override def futureLike[A](
      creator: js.Function2[js.Function1[A, _], js.Function1[Throwable, _], _]
  ): js.Thenable[A] = {
    new js.Promise[A](
      creator.asInstanceOf[
        js.Function2[js.Function1[A | js.Thenable[A], _], js.Function1[Throwable, _], _]
      ]
    )
  }

  override def flatMapFutureLike[A, B](
      futureLike: js.Thenable[A],
      f: js.Function1[A, js.Thenable[B]]
  ): js.Thenable[B] = {
    futureLike.`then`(f: js.Function1[A, B | js.Thenable[B]], js.undefined)
  }

  override def onCompleteFutureLike[A, B](
      futureLike: js.Thenable[A],
      success: js.Function1[A, B],
      failure: js.Function1[Throwable, B]
  ): Unit = {
    futureLike.`then`(
      success.asInstanceOf[js.Function1[A, B | js.Thenable[B]]],
      js.defined(failure).asInstanceOf[js.UndefOr[js.Function1[Any, B | js.Thenable[B]]]]
    )
    ()
  }
}
