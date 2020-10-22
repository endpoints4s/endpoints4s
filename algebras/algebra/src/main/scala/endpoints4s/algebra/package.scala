package endpoints4s

/** Algebra interfaces
  *
  * @groupname algebras Algebras
  * @groupname interpreters Interpreters
  */
package object algebra {

  type Documentation = Option[String]

  private[algebra] def unsupportedInterpreter(algebraVersion: String): Nothing =
    sys.error(
      s"Unsupported algebra version: $algebraVersion. Please update your interpreter dependency."
    )

}
