package endpoints4s

/** Possible restrictions on the numeric value used. Needs an instance for [[Ordering]] to check whether values are
  * valid w.r.t. to the properties.
  * @group operations
  */
case class NumericConstraints[A](
    minimum: Option[A] = None,
    exclusiveMinimum: Option[Boolean] = None,
    maximum: Option[A] = None,
    exclusiveMaximum: Option[Boolean] = None,
    multipleOf: Option[A] = None
)(implicit val ord: Ordering[A], mult: MultipleOf[A]) {
  import ord._

  override def toString: String =
    (p2s(minimum, "minimum") ++
      p2s(exclusiveMinimum, "exclusiveMinimum") ++
      p2s(maximum, "maximum") ++
      p2s(exclusiveMaximum, "exclusiveMaximum")).mkString(", ")

  /** Check whether the value satisfies all the constraints */
  def satisfiedBy(value: A): Boolean = {
    def checkMultipleOf(bound: A) = mult.multipleOf(bound, value)

    def checkMinimum(bound: A) =
      if (exclusiveMinimum.getOrElse(false)) bound < value
      else bound <= value

    def checkMaximum(bound: A) =
      if (exclusiveMaximum.getOrElse(false)) bound > value
      else bound >= value

    minimum.forall(checkMinimum) && maximum.forall(checkMaximum) && multipleOf.forall(
      checkMultipleOf
    )
  }

  private def p2s[B](opt: Option[B], name: String): List[String] =
    opt.map(v => s"$name:$v").toList
}
