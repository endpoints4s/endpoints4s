package endpoints4s

/** Possible restrictions on the numeric value used. Needs an instance for `Ordering` to check whether values are
  * valid w.r.t. to the properties.
  * @group operations
  */
final class NumericConstraints[A] private (
    val minimum: Option[A] = None,
    val exclusiveMinimum: Option[Boolean] = None,
    val maximum: Option[A] = None,
    val exclusiveMaximum: Option[Boolean] = None,
    val multipleOf: Option[A] = None
)(implicit ord: Ordering[A], mult: MultipleOf[A])
    extends Serializable {
  import ord._

  override def toString: String =
    (optToString(minimum, "minimum") ++
      optToString(exclusiveMinimum, "exclusiveMinimum") ++
      optToString(maximum, "maximum") ++
      optToString(exclusiveMaximum, "exclusiveMaximum") ++
      optToString(multipleOf, "multipleOf")).mkString(", ")

  override def equals(other: Any): Boolean =
    other match {
      case that: NumericConstraints[_] =>
        minimum == that.minimum &&
          exclusiveMinimum == that.exclusiveMinimum &&
          maximum == that.maximum &&
          exclusiveMaximum == that.exclusiveMaximum &&
          multipleOf == that.multipleOf
      case _ => false
    }

  override def hashCode(): Int =
    Hashing.hash(minimum, exclusiveMinimum, maximum, exclusiveMaximum, multipleOf)

  def withMinimum(minimum: Option[A]): NumericConstraints[A] =
    copy(minimum = minimum)

  def withExclusiveMinimum(exclusiveMinimum: Option[Boolean]): NumericConstraints[A] =
    copy(exclusiveMinimum = exclusiveMinimum)

  def withMaximum(maximum: Option[A]): NumericConstraints[A] =
    copy(maximum = maximum)

  def withExclusiveMaximum(exclusiveMaximum: Option[Boolean]): NumericConstraints[A] =
    copy(exclusiveMaximum = exclusiveMaximum)

  def withMultipleOf(multipleOf: Option[A]): NumericConstraints[A] =
    copy(multipleOf = multipleOf)

  private def copy(
      minimum: Option[A] = minimum,
      exclusiveMinimum: Option[Boolean] = exclusiveMinimum,
      maximum: Option[A] = maximum,
      exclusiveMaximum: Option[Boolean] = exclusiveMaximum,
      multipleOf: Option[A] = multipleOf
  ) = new NumericConstraints[A](minimum, exclusiveMinimum, maximum, exclusiveMaximum, multipleOf)

  /** Check whether the value satisfies all the constraints */
  def satisfiedBy(value: A): Boolean = {
    def checkMultipleOf(bound: A) = mult.multipleOf(value, bound)

    def checkMinimum(bound: A) =
      if (exclusiveMinimum.getOrElse(false)) bound < value
      else bound <= value

    def checkMaximum(bound: A) =
      if (exclusiveMaximum.getOrElse(false)) bound > value
      else bound >= value

    minimum.forall(checkMinimum) &&
    maximum.forall(checkMaximum) &&
    multipleOf.forall(checkMultipleOf)
  }

  private def optToString[B](opt: Option[B], name: String): List[String] =
    opt.map(v => s"$name:$v").toList
}

object NumericConstraints {

  def apply[A](implicit ord: Ordering[A], mult: MultipleOf[A]): NumericConstraints[A] =
    new NumericConstraints[A]()

}
