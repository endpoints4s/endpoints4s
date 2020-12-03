package endpoints4s

/** Type class that checks whether something is a multiple of another numeric value. Added since the std-lib Numeric
  * does not have a modulo function.
  */
trait MultipleOf[A] {
  def multipleOf(a: A, b: A): Boolean
}

object MultipleOf {
  @inline def apply[A](implicit instance: MultipleOf[A]): MultipleOf[A] = instance

  implicit val intInstance: MultipleOf[Int] = (a, b) => a % b == 0
  implicit val longInstance: MultipleOf[Long] = (a, b) => a % b == 0
  implicit val floatInstance: MultipleOf[Float] = (a, b) => a % b == 0
  implicit val doubleInstance: MultipleOf[Double] = (a, b) => a % b == 0
  implicit val bigdecimalInstance: MultipleOf[BigDecimal] = (a, b) => a % b == 0

}
