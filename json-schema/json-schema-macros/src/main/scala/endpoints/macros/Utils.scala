package endpoints.macros

import scala.reflect.macros.blackbox

trait Utils {

  val c: blackbox.Context

  import c.universe._

  private[macros] implicit class SymbolOps(ts: Symbol) {
    final def classSymbolOpt: Option[ClassSymbol] =
      if (ts.isClass) Some(ts.asClass) else None

    final def isCaseClass: Boolean =
      classSymbolOpt.exists(_.isCaseClass)

    final def isSealedClass: Boolean =
      classSymbolOpt.exists(_.isSealed)

    final def tpe: Type =
      ts.asType.toType
  }

  private[macros] implicit class MethodSymbolOps(ms: MethodSymbol) {
    final def returnTypeIn(tpe: Type): Type =
      ms.typeSignatureIn(tpe) match { case NullaryMethodType(rt) => rt }
  }

  private[macros] implicit class TypeOps(t: Type) {
    final def caseClassParams: Iterable[MethodSymbol] =
      t.decls.collect { case m: MethodSymbol if m.isCaseAccessor => m.asMethod }
  }
}
