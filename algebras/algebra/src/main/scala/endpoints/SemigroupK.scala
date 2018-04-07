package endpoints

import scala.language.higherKinds

trait SemigroupK[F[_]] {

  def add[A, B](fa: F[A], fb: F[B])(implicit tupler: Tupler[A, B]): F[tupler.Out]

}

trait SemigroupKSyntax {
  implicit class SemigroupKSyntax[A, F[_]](val f: F[A])(implicit ev: SemigroupK[F]) {
    def ++[B](other: F[B])(implicit tupler: Tupler[A, B]): F[tupler.Out] = ev.add(f,other)
  }
}