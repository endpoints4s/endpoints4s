package endpoints

// TODO Replace with shapeless’s adjoin
trait FlatConcat[A, B] {
  type Out
  def apply(a: A, b: B): Out
  def unapply(out: Out): (A, B)
}

trait FlatConcatLowImplicits {
  type Aux[A, B, Out0] = FlatConcat[A, B] { type Out = Out0 }
  implicit def aB[A, B]: Aux[A, B, (A, B)] = new FlatConcat[A, B] {
    type Out = (A, B)
    def apply(a: A, b: B): (A, B) = (a, b)
    def unapply(out: (A, B)): (A, B) = out
  }
}

object FlatConcat extends FlatConcatLowImplicits {
  implicit val unitUnit: Aux[Unit, Unit, Unit] = new FlatConcat[Unit, Unit] {
    type Out = Unit
    def apply(a: Unit, b: Unit): Unit = ()
    def unapply(out: Out): (Unit, Unit) = ((), ())
  }
  implicit def unitA[A]: Aux[Unit, A, A] = new FlatConcat[Unit, A] {
    type Out = A
    def apply(a: Unit, b: A): A = b
    def unapply(out: Out): (Unit, A) = ((), out)
  }
  implicit def AUnit[A]: Aux[A, Unit, A] = new FlatConcat[A, Unit] {
    type Out = A
    def apply(a: A, b: Unit): A = a
    def unapply(out: Out): (A, Unit) = (out, ())
  }
}