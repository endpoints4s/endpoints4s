package endpoints

import scala.language.higherKinds

trait Semigroupal[F[_]] {

  def product[A, B](fa: F[A], fb: F[B])(implicit tupler: Tupler[A, B]): F[tupler.Out]

}

trait SemigroupalSyntax {
  implicit class SemigroupalSyntax[A, F[_]](val f: F[A])(implicit ev: Semigroupal[F]) {
    def ++[B](other: F[B])(implicit tupler: Tupler[A, B]): F[tupler.Out] = ev.product(f,other)
  }
}