package endpoints

import scala.language.higherKinds

/** Defines way to transform give type constructor F */
trait InvariantFunctor[F[_]] {

  def xmap[From, To](f: F[From], map: From => To, contramap: To => From): F[To]

}

trait InvariantFunctorSyntax {
  implicit class InvariantFunctorSyntax[A, F[_]](val f: F[A])(implicit ev: InvariantFunctor[F]) {
    def xmap[To](map: A => To, contramap: To => A): F[To] = ev.xmap(f, map, contramap)

    //TODO add as[CaseClass] macro
  }
}
