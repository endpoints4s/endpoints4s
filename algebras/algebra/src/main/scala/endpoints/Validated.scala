package endpoints

/**
  * A validated value of type `A` can either be `Valid` or `Invalid`
  * @tparam A Type of the validated value
  */
sealed trait Validated[+A] {

  /**
    * Transforms this validated value into a value of type `B`
    */
  def fold[B](valid: A => B, invalid: Seq[String] => B): B = this match {
    case Valid(value)    => valid(value)
    case Invalid(errors) => invalid(errors)
  }

  /**
    * Transforms a valid value of type `A` into a valid value of type `B`.
    * An invalid value is returned as it is.
    */
  def map[B](f: A => B): Validated[B] = this match {
    case Valid(value)     => Valid(f(value))
    case invalid: Invalid => invalid
  }

  /**
    * Transforms the error list of an invalid value.
    * A valid value is returned as it is.
    */
  def mapErrors(f: Seq[String] => Seq[String]): Validated[A] = this match {
    case valid: Valid[A] => valid
    case Invalid(errors) => Invalid(f(errors))
  }

  /**
    * Subsequently validates this valid value.
    * An invalid value is returned as it is.
    */
  def flatMap[B](f: A => Validated[B]): Validated[B] = this match {
    case Valid(value)     => f(value)
    case invalid: Invalid => invalid
  }

  /**
    * Tuples together two validated values
    *
    * @see [[tuple]]
    */
  def zip[B](that: Validated[B]): Validated[(A, B)] = this.tuple(that)

  /**
    * Tuples together two validated values and tries to return a flat tuple instead of nested tuples. Also strips
    * out `Unit` values in the tuples.
    *
    * If `this` and `that` are both invalid values, this operation returns an `Invalid` value containing both
    * `this` error messages and `that` error messages.
    *
    * @see [[Tupler]]
    */
  def tuple[A0 >: A, B](that: Validated[B])(implicit tupler: Tupler[A0, B]): Validated[tupler.Out] = (this, that) match {
    case (Valid(a), Valid(b))             => Valid(tupler(a, b))
    case (_: Valid[A], invalid: Invalid)  => invalid
    case (invalid: Invalid, _: Valid[B])  => invalid
    case (Invalid(errs1), Invalid(errs2)) => Invalid(errs1 ++ errs2)
  }

  /**
    * Transforms this `Validated[A]` value into an `Either[Seq[String], A]` value.
    */
  def toEither: Either[Seq[String], A] = this match {
    case Invalid(errors) => Left(errors)
    case Valid(a)        => Right(a)
  }

}

/** A valid value of type `A` */
case class Valid[A](value: A) extends Validated[A]
/** A list of validation errors */
case class Invalid(errors: Seq[String]) extends Validated[Nothing]

object Invalid {
  /** An invalid value due to a single error */
  def apply(error: String): Invalid = Invalid(error :: Nil)
}

object Validated {

  /**
    * Turns `None` into an invalid value, using the given `error` message.
    * Turns a `Some[A]` value into a `Valid[A]` value.
    */
  def fromOption[A](maybeA: Option[A])(error: => String): Validated[A] = maybeA match {
    case Some(value) => Valid(value)
    case None        => Invalid(error)
  }

  /**
    * Turns an `Either[Seq[String], A]` into a `Validated[A]`
    */
  def fromEither[A](either: Either[Seq[String], A]): Validated[A] = either match {
    case Left(errors) => Invalid(errors)
    case Right(a)     => Valid(a)
  }

}
