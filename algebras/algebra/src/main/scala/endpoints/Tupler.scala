package endpoints

/**
  * Defines a strategy for tupling `A` and `B` values, according to types `A` and `B`.
  *
  * The actual implementation avoids nested tuples and eliminates `Unit`, so that instead of ending with, e.g.,
  * the following type:
  *
  * {{{
  *   ((Unit, Int), (((Unit, Unit), String)))
  * }}}
  *
  * We just get:
  *
  * {{{
  *   (Int, String)
  * }}}
  *
  * The following rules are implemented (by increasing priority):
  *  - A, B           -> (A, B)
  *  - A, (B, C)      -> (A, B, C)
  *  - (A, B), C      -> (A, B, C)
  *  - (A, B), (C, D) -> (A, B, C, D)
  *  - A, Unit        -> A
  *  - Unit, A        -> A
  */
//#definition
trait Tupler[A, B] {
  type Out
//#definition
  def apply(a: A, b: B): Out
  def unapply(out: Out): (A, B)
//#definition
}
//#definition

object Tupler extends Tupler4

trait Tupler1 {
  type Aux[A, B, Out0] = Tupler[A, B] { type Out = Out0 }

  implicit def ab[A, B]: Aux[A, B, (A, B)] = new Tupler[A, B] {
    type Out = (A, B)
    def apply(a: A, b: B): (A, B) = (a, b)
    def unapply(out: (A, B)): (A, B) = out
  }

}

trait Tupler2 extends Tupler1 {

  implicit def left[A, B, C]: Aux[A, (B, C), (A, B, C)] =
    new Tupler[A, (B, C)] {
      type Out = (A, B, C)
      def apply(a: A, bc: (B, C)): (A, B, C) = (a, bc._1, bc._2)
      def unapply(out: (A, B, C)): (A, (B, C)) = {
        val (a, b, c) = out
        (a, (b, c))
      }
    }

  implicit def right[A, B, C]: Aux[(A, B), C, (A, B, C)] =
    new Tupler[(A, B), C] {
      type Out = (A, B, C)
      def apply(ab: (A, B), c: C): (A, B, C) = (ab._1, ab._2, c)
      def unapply(out: (A, B, C)): ((A, B), C) = {
        val (a, b, c) = out
        ((a, b), c)
      }
    }

}

trait Tupler3 extends Tupler2 {

  implicit def leftRight[A, B, C, D]: Aux[(A, B), (C, D), (A, B, C, D)] =
    new Tupler[(A, B), (C, D)] {
      type Out = (A, B, C, D)
      def apply(ab: (A, B), cd: (C, D)): (A, B, C, D) = (ab._1, ab._2, cd._1, cd._2)
      def unapply(out: (A, B, C, D)): ((A, B), (C, D)) = {
        val (a, b, c, d) = out
        ((a, b), (c, d))
      }
    }

  implicit def leftUnit[A]: Aux[Unit, A, A] = new Tupler[Unit, A] {
    type Out = A
    def apply(a: Unit, b: A): A = b
    def unapply(out: Out): (Unit, A) = ((), out)
  }

}

trait Tupler4 extends Tupler3 {

  implicit def rightUnit[A]: Aux[A, Unit, A] = new Tupler[A, Unit] {
    type Out = A
    def apply(a: A, b: Unit): A = a
    def unapply(out: Out): (A, Unit) = (out, ())
  }

}
