package endpoints4s.fetch.thenable

import endpoints4s.fetch.FutureLike

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
}
