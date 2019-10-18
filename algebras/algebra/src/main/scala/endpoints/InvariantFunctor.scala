package endpoints

import scala.language.higherKinds

/** Defines ways to transform a given type constructor F */
trait InvariantFunctor[F[_]] {
  /**
    * Transforms an `F[A]` value into an `F[B]` value given a pair
    * of functions from `A` to `B` and from `B` to `A`.
    *
    * @see [[http://julienrf.github.io/endpoints/algebras/endpoints.html#transforming-and-refining-url-constituents Some examples]]
    */
  def xmap[A, B](fa: F[A], f: A => B, g: B => A): F[B]
}

trait InvariantFunctorSyntax {
  /**
    * Extension methods for values of type `F[A]` for which there is an implicit
    * `InvariantFunctor[F]` instance.
    */
  implicit class InvariantFunctorSyntax[A, F[_]](val fa: F[A])(implicit ev: InvariantFunctor[F]) {
    /**
      * Transforms an `F[A]` value into an `F[B]` value given a pair
      * of functions from `A` to `B` and from `B` to `A`.
      *
      * @see [[http://julienrf.github.io/endpoints/algebras/endpoints.html#transforming-and-refining-url-constituents Some examples]]
      */
    def xmap[B](f: A => B)(g: B => A): F[B] = ev.xmap(fa, f, g)
  }
}

/** Given a type constructor `F`, a partial function `A => Validated[B]`
  * and a total function `B => A`, turns an `F[A]` into an `F[B]`.
  *
  * A partial invariant functor is an invariant functor whose covariant
  * transformation function is total (ie, `A => Valid[B]`).
  */
trait PartialInvariantFunctor[F[_]] extends InvariantFunctor[F] {
  /**
    * Transforms an `F[A]` value into an `F[B]` value given a partial function
    * from `A` to `B`, and a total function from `B` to `A`.
    *
    * This is useful to ''refine'' the type `A` into a possibly smaller type `B`.
    *
    * @see [[http://julienrf.github.io/endpoints/algebras/endpoints.html#transforming-and-refining-url-constituents Some examples]]
    */
  def xmapPartial[A, B](fa: F[A], f: A => Validated[B], g: B => A): F[B]
  def xmap[A, B](fa: F[A], f: A => B, g: B => A): F[B] = xmapPartial[A, B](fa, a => Valid(f(a)), g)
}

trait PartialInvariantFunctorSyntax extends InvariantFunctorSyntax {
  implicit class PartialInvariantFunctorSyntax[A, F[_]](val fa: F[A])(implicit ev: PartialInvariantFunctor[F]) {
    /**
      * Transforms an `F[A]` value into an `F[B]` value given a partial function
      * from `A` to `B`, and a total function from `B` to `A`.
      *
      * This is useful to ''refine'' the type `A` into a possibly smaller type `B`.
      *
      * @see [[http://julienrf.github.io/endpoints/algebras/endpoints.html#transforming-and-refining-url-constituents Some examples]]
      */
    def xmapPartial[B](f: A => Validated[B])(g: B => A): F[B] = ev.xmapPartial(fa, f, g)
  }
}
