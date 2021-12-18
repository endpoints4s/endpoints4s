package endpoints4s.fetch

import scala.scalajs.js

trait FutureLike {

  type FutureLike[A]

  def futureLike[A](
      creator: js.Function2[js.Function1[A, _], js.Function1[Throwable, _], _]
  ): FutureLike[A]
}
