package endpoints4s

class TuplerTests {

  def tupling[A, B](a: A, b: B)(implicit tupler: Tupler[A, B]): tupler.Out =
    tupler(a, b)

  def forall[A, B, C, D, E, F, G, H, I](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I): Unit = {
    // Left/Right unit
    tupling((), ()): Unit
    tupling((), a): A
    tupling(a, ()): A

    // Tupler
    tupling(a, b): (A, B)
    tupling(a, (b, c)): (A, B, C)
    tupling((a, b), (c, d)): (A, B, C, D)
    tupling(a, (b, c, d, e)): (A, B, C, D, E)
    tupling((a, b), (c, d, e)): (A, B, C, D, E)

    // TuplerAppend
    tupling((a, b), c): (A, B, C)
    tupling((a, b, c), d): (A, B, C, D)
    tupling((a, b, c, d), e): (A, B, C, D, E)
    tupling((a, b, c, d, e), f): (A, B, C, D, E, F)
    tupling((a, b, c, d, e, f), g): (A, B, C, D, E, F, G)
    tupling((a, b, c, d, e, f, g), h): (A, B, C, D, E, F, G, H)
    tupling((a, b, c, d, e, f, g, h), i): (A, B, C, D, E, F, G, H, I)

    // Stability tests
    tupling(a, (b, c, d))                 : (A, (B, C, D))
    tupling((a, b, c), (d, e))            : (A, B, C, (D, E))
    tupling((a, b, c, d), (e, f))         : (A, B, C, D, (E, F))
    tupling((a, b, c), (d, e, f))         : (A, B, C, (D, E, F))
    tupling(a, (b, c, d, e, f))           : (A, (B, C, D, E, F))
    tupling((a, b), (c, d, e, f))         : ((A, B), C, D, E, F)
    tupling((a, b, c, d, e), (f, g))      : (A, B, C, D, E, (F, G))
    tupling((a, b, c, d), (e, f, g))      : (A, B, C, D, (E, F, G))
    tupling((a, b, c), (d, e, f, g))      : (A, B, C, (D, E, F, G))
    tupling((a, b), (c, d, e, f, g))      : (A, B, (C, D, E, F, G))
    tupling(a, (b, c, d, e, f, g))        : (A, (B, C, D, E, F, G))
    tupling((a, b, c, d, e, f), (g, h))   : (A, B, C, D, E, F, (G, H))
    tupling((a, b, c, d, e), (f, g, h))   : (A, B, C, D, E, (F, G, H))
    tupling((a, b, c, d), (e, f, g, h))   : (A, B, C, D, (E, F, G, H))
    tupling((a, b, c), (d, e, f, g, h))   : (A, B, C, (D, E, F, G, H))
    tupling((a, b), (c, d, e, f, g, h))   : (A, B, (C, D, E, F, G, H))
    tupling(a, (b, c, d, e, f, g, h))     : (A, (B, C, D, E, F, G, H))
    tupling(a, (b, c, d, e, f, g, h, i))  : (A, (B, C, D, E, F, G, H, I))
    tupling((a, b), (c, d, e, f, g, h, i)): (A, B, (C, D, E, F, G, H, I))
    tupling((a, b, c), (d, e, f, g, h, i)): (A, B, C, (D, E, F, G, H, I))
    tupling((a, b, c, d), (e, f, g, h, i)): (A, B, C, D, (E, F, G, H, I))
    tupling((a, b, c, d, e), (f, g, h, i)): (A, B, C, D, E, (F, G, H, I))
    tupling((a, b, c, d, e, f), (g, h, i)): (A, B, C, D, E, F, (G, H, I))
    tupling((a, b, c, d, e, f, g), (h, i)): (A, B, C, D, E, F, G, (H, I))

    // Fin
    ()
  }

}
