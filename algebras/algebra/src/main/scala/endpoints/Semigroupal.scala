package endpoints

/** Ability for a type constructor `F` to combine together two values of type `F[A]` and `F[B]` into a value of type `F[(A, B)]` */
trait Semigroupal[F[_]] {

  def product[A, B](fa: F[A], fb: F[B])(
      implicit tupler: Tupler[A, B]
  ): F[tupler.Out]

}

/** Provides extension methods for values of type [[Semigroupal]] */
trait SemigroupalSyntax {
  implicit class SemigroupalSyntax[A, F[_]](val f: F[A])(
      implicit ev: Semigroupal[F]
  ) {
    def ++[B](other: F[B])(implicit tupler: Tupler[A, B]): F[tupler.Out] =
      ev.product(f, other)
  }
}
