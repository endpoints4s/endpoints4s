package endpoints

import scala.language.higherKinds

/** Defines ways to transform a given type constructor F */
trait InvariantFunctor[F[_]] {

  def xmap[From, To](f: F[From], map: From => To, contramap: To => From): F[To]

}

trait InvariantFunctorSyntax {
  implicit class InvariantFunctorSyntax[A, F[_]](val f: F[A])(implicit ev: InvariantFunctor[F]) {
    def xmap[To](map: A => To, contramap: To => A): F[To] = ev.xmap(f, map, contramap)

    //TODO add as[CaseClass] macro
  }
}

/** Given a type constructor `F`, a partial function `A => Option[B]`
  * and a total function `B => A`, turns an `F[A]` into an `F[B]`.
  *
  * A partial invariant functor is an invariant functor whose covariant
  * transformation function is total (ie, `A => Some[B]`).
  */
trait PartialInvariantFunctor[F[_]] extends InvariantFunctor[F] {
  def xmapPartial[A, B](fa: F[A], f: A => Option[B], g: B => A): F[B]
  def xmap[A, B](fa: F[A], f: A => B, g: B => A): F[B] = xmapPartial[A, B](fa, a => Some(f(a)), g)
}

trait PartialInvariantFunctorSyntax extends InvariantFunctorSyntax {
  implicit class PartialInvariantFunctorSyntax[A, F[_]](val fa: F[A])(implicit ev: PartialInvariantFunctor[F]) {
    def xmapPartial[B](f: A => Option[B])(g: B => A): F[B] = ev.xmapPartial(fa, f, g)
  }
}
