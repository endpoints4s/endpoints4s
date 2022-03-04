package endpoints4s

import scala.collection.compat._
import scala.util.{Failure, Success, Try}

/** A validated value of type `A` can either be `Valid` or `Invalid`
  * @tparam A Type of the validated value
  */
sealed trait Validated[+A] {

  /** Transforms this validated value into a value of type `B`
    */
  def fold[B](valid: A => B, invalid: Seq[String] => B): B =
    this match {
      case Valid(value)    => valid(value)
      case Invalid(errors) => invalid(errors)
    }

  /** Transforms a valid value of type `A` into a valid value of type `B`.
    * An invalid value is returned as it is.
    */
  def map[B](f: A => B): Validated[B] =
    this match {
      case Valid(value)     => Valid(f(value))
      case invalid: Invalid => invalid
    }

  /** Transforms the error list of an invalid value.
    * A valid value is returned as it is.
    */
  def mapErrors(f: Seq[String] => Seq[String]): Validated[A] =
    this match {
      case valid: Valid[A] => valid
      case Invalid(errors) => Invalid(f(errors))
    }

  /** Subsequently validates this valid value.
    * An invalid value is returned as it is.
    */
  def flatMap[B](f: A => Validated[B]): Validated[B] =
    this match {
      case Valid(value)     => f(value)
      case invalid: Invalid => invalid
    }

  /** Tuples together two validated values and tries to return a flat tuple instead of nested tuples. Also strips
    * out `Unit` values in the tuples.
    *
    * If `this` and `that` are both invalid values, this operation returns an `Invalid` value containing both
    * `this` error messages and `that` error messages.
    *
    * @see [[Tupler]]
    */
  def zip[A0 >: A, B](that: Validated[B])(implicit
      tupler: Tupler[A0, B]
  ): Validated[tupler.Out] =
    (this, that) match {
      case (Valid(a), Valid(b))             => Valid(tupler(a, b))
      case (_: Valid[A], invalid: Invalid)  => invalid
      case (invalid: Invalid, _: Valid[B])  => invalid
      case (Invalid(errs1), Invalid(errs2)) => Invalid(errs1 ++ errs2)
    }

  /** Transforms this `Validated[A]` value into an `Either[Seq[String], A]` value.
    */
  def toEither: Either[Seq[String], A] =
    this match {
      case Invalid(errors) => Left(errors)
      case Valid(a)        => Right(a)
    }

}

/** A valid value of type `A` */
case class Valid[+A](value: A) extends Validated[A]

/** A list of validation errors */
case class Invalid(errors: Seq[String]) extends Validated[Nothing]

object Invalid {

  /** An invalid value due to a single error */
  def apply(error: String): Invalid = Invalid(error :: Nil)
}

object Validated {

  /** Turns `None` into an invalid value, using the given `error` message.
    * Turns a `Some[A]` value into a `Valid[A]` value.
    */
  def fromOption[A](maybeA: Option[A])(error: => String): Validated[A] =
    maybeA match {
      case Some(value) => Valid(value)
      case None        => Invalid(error)
    }

  /** Turns an `Either[Seq[String], A]` into a `Validated[A]`
    */
  def fromEither[A](either: Either[Seq[String], A]): Validated[A] =
    either match {
      case Left(errors) => Invalid(errors)
      case Right(a)     => Valid(a)
    }

  /** Turns a `Success[A]` into a `Valid[A]`
    *
    * Turns a `Failure[A]` into an invalid value, using the exception message as an 'error' message
    */
  def fromTry[A](tryA: Try[A]): Validated[A] =
    tryA match {
      case Success(value) => Valid(value)
      case Failure(error) => Invalid(error.getMessage)
    }

  /** Turns a collection of valid values into a valid collection of values.
    * If all the values in the collection are `Valid`, the result will be `Valid`.
    * If there is at least one `Invalid`, the result will be an `Invalid` containing the error messages of all `Invalid`s
    * If the collection is empty, it will return a `Valid` empty collection.
    *
    * @see [[scala.concurrent.Future.sequence]]
    *
    * @param in   the collection of `Validated` values that will be sequenced
    * @tparam A   the type of the `Validated` values (e.g. `Int`)
    * @tparam CC  the type of the collection of `Validated` values (e.g. `List`)
    * @return     the `Validated` of the resulting collection
    */
  def sequence[A, CC[X] <: IterableOnce[X]](
      in: CC[Validated[A]]
  )(implicit bf: BuildFrom[CC[Validated[A]], A, CC[A]]): Validated[CC[A]] = {
    val builder = bf.newBuilder(in)

    in.iterator
      .foldLeft[Validated[Unit]](Valid(())) { case (folded, next) =>
        folded.zip(next).map { n => builder += n; () }
      }
      .map(_ => builder.result())
  }

  /** Validates a collection of values using the provided function `A => Validated[B]` to validate every value.
    * If all the values in the collection are `Valid`, the result will be `Valid`.
    * If there is at least one `Invalid`, the result will be an `Invalid` containing the error messages of all `Invalid`s
    * If the collection is empty, it will return a `Valid` empty collection.
    *
    *  {{{
    *    val myValidatedList = Validated.traverse(myList)(x => myValidateFunc(x))
    *  }}}
    *
    * @see [[scala.concurrent.Future.traverse]]
    *
    * @param in        the collection of values to be validated with the provided function
    * @param fn        the validation function
    * @tparam A        the type of the values to be validated (e.g. `String`)
    * @tparam B        the type of the returned validated values (e.g. `Int`)
    * @tparam CC       the type of the collection of `Validated` (e.g. `List`)
    * @return          the `Validated` of the resulting collection
    */
  def traverse[A, B, CC[X] <: IterableOnce[X]](
      in: CC[A]
  )(fn: A => Validated[B])(implicit bf: BuildFrom[CC[A], B, CC[B]]): Validated[CC[B]] = {
    val builder = bf.newBuilder(in)

    in.iterator
      .foldLeft[Validated[Unit]](Valid(())) { case (folded, next) =>
        folded.zip(fn(next)).map { n => builder += n; () }
      }
      .map(_ => builder.result())

  }

}
