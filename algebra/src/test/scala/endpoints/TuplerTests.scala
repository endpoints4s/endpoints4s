package endpoints

class TuplerTests {

  def tupling[A, B](a: A, b: B)(implicit tupler: Tupler[A, B]): tupler.Out = tupler(a, b)

  def forall[A, B, C, D](a: A, b: B, c: C, d: D): Unit = {
    tupling(a, b): (A, B)
    tupling(a, (b, c)): (A, B, C)
    tupling((a, b), c): (A, B, C)
    tupling((a, b), (c, d)): (A, B, C, D)
    tupling(a, ()): A
    tupling((), a): A
    tupling((), ()): Unit
  }

}
