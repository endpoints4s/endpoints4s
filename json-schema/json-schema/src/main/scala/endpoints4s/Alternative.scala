package endpoints4s

import AlternativeTypes._ 

//Note this is mostly for discussion for potential syntax support in endpoints4s to deal with nested either. It can be thrown out of the MR as it has no dependencies

//Anologous to Tupler but for Eithers 
//An either is fully qualified by its inject and fold. So to give users convenient albeit slightly odd syntax that folds and inject up to some arity (currently 4)
//This can be thrown out entirely (probably tupler as well) when we're fully on scala 3 and no longer offer 2 support (so a long while from now)
trait Alternative[A,B] {
  type Clauses[_]

  //These are userfriendly methods that can be used. As of right now it's very painful to deal with endpoints with lots of either nesting coming from `orElse`. This way a user doesn't need to have the sealed trait of the correct arity in scope 
  def inject: Clauses[Either[A,B]]
  def fold[Result](either:Either[A,B])(clauses: Clauses[Result]):Result 
  
}

object AlternativeTypes {
  //Alternatively this could be a trait but I think it would be more annoying at usage site, requiring creation of anonymous instances with `new` or having an apply in which case it boils down to the same thing really. 
  case class Clauses2[A, B, Target](
    first: A => Target,
    second: B => Target 
  )
  case class Clauses3[A, B, C, Target](
    first: A => Target,
    second: B => Target,
    third: C => Target,
  )
  case class Clauses4[A, B, C, D, Target](
    first: A => Target,
    second: B => Target,
    third: C => Target,
    fourth: D => Target, 
  )

  //Very common fold Operations can be offered here like Embedding into Right Chained either form e.g. Either[A,Either[B,Either[C,D]] or merge to a common super type (sealed trait injeciton)
}

trait Alternative2 {
  type Aux[A,B, Clauses0[_]] = Alternative[A,B] {
    type Clauses[Target] = Clauses0[Target]
  }

  //We have no partial type application, this is what kind projector does under the hood. If we want to offer  `Alternative` to our users we'll have to think on how to do that 
  implicit def alternative2[A,B]: Aux[A,B, ({type L[T] = Clauses2[A,B,T]})#L] = new Alternative[A,B] {
    type Clauses[Target] = Clauses2[A,B,Target]
    //type SealedTraitRepr = Either2[A,B]

    override def inject:Clauses[Either[A,B]] = Clauses2(
      first = a => Left(a),
      second = b => Right(b),
    )

    override def fold[Result](either:Either[A,B])(clauses: Clauses[Result]):Result = either.fold(clauses.first, clauses.second)

  }
}

trait Alternative3 extends Alternative2 {

  implicit def alternative1Or2[A,B,C]: Aux[A,Either[B,C], ({type L[T] = Clauses3[A,B,C,T]})#L] = new Alternative[A,Either[B,C]] {
    type Clauses[Target] = Clauses3[A,B,C,Target]

    override def inject:Clauses[Either[A,Either[B,C]]] = Clauses3(
      first = a => Left(a),
      second = b => Right(Left(b)),
      third = c => Right(Right(c))
    )

    override def fold[Result](either:Either[A,Either[B,C]])(clauses: Clauses[Result]):Result = either.fold(clauses.first, _.fold(clauses.second, clauses.third))

  }

  implicit def alternative2Or1[A,B,C]: Aux[Either[A,B],C, ({type L[T] = Clauses3[A,B,C,T]})#L] = new Alternative[Either[A,B],C] {
    type Clauses[Target] = Clauses3[A,B,C,Target]

    override def inject:Clauses[Either[Either[A,B],C]] = Clauses3(
      first = a => Left(Left(a)),
      second = b => Left(Right(b)),
      third = c => Right(c)
    )

    override def fold[Result](either:Either[Either[A,B],C])(clauses: Clauses[Result]):Result = either.fold(_.fold(clauses.first, clauses.second), clauses.third)

  } 
}

//I didn't implement all layouts yet. 1or3 is the most common arising from chained `orElse` 
trait Alternative4 extends Alternative3 {

  implicit def alternative2Or2[A,B,C,D]: Aux[Either[A,B],Either[C,D], ({type L[T] = Clauses4[A,B,C,D,T]})#L] = new Alternative[Either[A,B],Either[C,D]] {
    type Clauses[Target] = Clauses4[A,B,C,D,Target]

    override def inject:Clauses[Either[Either[A,B],Either[C,D]]] = Clauses4(
      first = a => Left(Left(a)),
      second = b => Left(Right(b)),
      third = c => Right(Left(c)),
      fourth = d => Right(Right(d))
    )

    override def fold[Result](either:Either[Either[A,B],Either[C,D]])(clauses: Clauses[Result]):Result = either.fold(_.fold(clauses.first, clauses.second), _.fold(clauses.third, clauses.fourth))

  }


  implicit def alternative1Or3[A,B,C,D]: Aux[A,Either[B,Either[C,D]], ({type L[T] = Clauses4[A,B,C,D,T]})#L] = new Alternative[A,Either[B,Either[C,D]]] {
    type Clauses[Target] = Clauses4[A,B,C,D,Target]

    override def inject:Clauses[Either[A,Either[B,Either[C,D]]]] = Clauses4(
      first = a => Left(a),
      second = b => Right(Left(b)),
      third = c => Right(Right(Left(c))),
      fourth = d => Right(Right(Right(d)))
    )

    override def fold[Result](either:Either[A,Either[B,Either[C,D]]])(clauses: Clauses[Result]):Result = either.fold(
      clauses.first,
      _.fold(
        clauses.second,
        _.fold(
          clauses.third,
          clauses.fourth,
        )
      )
    )

  }

}

object Alternative extends Alternative4

