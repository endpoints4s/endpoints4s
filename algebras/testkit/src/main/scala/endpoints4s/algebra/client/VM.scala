package endpoints4s.algebra.client

object VM extends Enumeration {
  type VM = Value
  val JS, JVM = Value

  def current: VM = if (System.getProperty("java.vm.name") == "Scala.js") JS else JVM
}
