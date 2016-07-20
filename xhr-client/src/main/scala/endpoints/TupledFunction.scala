package endpoints

trait TupledFunction[A, B] extends (A => B) {

  def apply()(implicit ev: Unit =:= A): B = apply(())

  def apply[A1, A2](a1: A1, a2: A2)(implicit ev: (A1, A2) =:= A): B = apply((a1, a2))

  def apply[A1, A2, A3](a1: A1, a2: A2, a3: A3)(implicit ev: ((A1, A2), A3) =:= A): B = apply(((a1, a2), a3))

  def apply[A1, A2, A3, A4](a1: A1, a2: A2, a3: A3, a4: A4)(implicit ev: (((A1, A2), A3), A4) =:= A): B = apply((((a1, a2), a3), a4))

}

object TupledFunction {
  def apply[A, B](f: A => B): TupledFunction[A, B] =
    new TupledFunction[A, B] {
      def apply(a: A) = f(a)
    }
}
