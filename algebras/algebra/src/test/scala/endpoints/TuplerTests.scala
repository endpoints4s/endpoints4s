package endpoints

class TuplerTests {

  def tupling[A, B](a: A, b: B)(implicit tupler: Tupler[A, B]): tupler.Out = tupler(a, b)

  def forall[A, B, C, D, E](a: A, b: B, c: C, d: D, e: E): Unit = {
    tupling(a, b): (A, B)
    tupling(a, (b, c)): (A, B, C)
    tupling((a, b), c): (A, B, C)
    tupling((a, b), (c, d)): (A, B, C, D)
    tupling(a, (b, c, d, e)): (A, B, C, D, E)
    tupling((a, b), (c, d, e)): (A, B, C, D, E)
    tupling((a, b, c), d): (A, B, C, D)
    tupling((a, b, c, d), e): (A, B, C, D, E)
    tupling(a, ()): A
    tupling((), a): A
    tupling((), ()): Unit
  }

}
