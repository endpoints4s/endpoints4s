package endpoints.macros

import scala.reflect.macros.blackbox

class Macros(val c: blackbox.Context) extends Utils {

  import c.universe._

  final def genericJsonSchemaImpl[A: c.WeakTypeTag]: c.Tree = {
    val A = weakTypeOf[A]
    findJsonSchema(A)
      .map { t => println(s"JsonSchema[$A] found: $t"); t }
      .getOrElse {
      c.abort(c.enclosingPosition, s"Can't derive JsonSchema[$A]")
    }
  }

  private def findJsonSchema(tpe: Type, allowImplicitSearch: Boolean = false): Option[c.Tree] = {
    println(s"findJsonSchema($tpe)")
    if(primitiveTypes.exists(tpe <:< _) || tpe <:< typeOf[Seq[_]]) {
      findImplicit(tq"JsonSchema[$tpe]")
    } else {
      (if (allowImplicitSearch) findImplicit(tq"JsonSchema[$tpe]") else None) orElse
        findRecord(tpe) orElse
        findTagged(tpe)
    }
  }

  private val primitiveTypes: Seq[Type] = Seq(
    typeOf[String],
    typeOf[Int],
    typeOf[Long],
    typeOf[BigDecimal],
    typeOf[Float],
    typeOf[Double],
    typeOf[Boolean]
  )

  private def findRecord(tpe: Type): Option[c.Tree] = {
    if(tpe.typeSymbol.isModuleClass) {

      val t = q"invmapRecord[_root_.scala.Unit, $tpe](emptyRecord, _ => ${tpe.typeSymbol.asClass.companionSymbol}, _ => ())"
      Some(namedJsonSchemaTree(tpe, t))

    } else if(tpe.typeSymbol.isCaseClass) {
      val paramsSeq = tpe.caseClassParams
      if (paramsSeq.isEmpty) {

        val t = q"invmapRecord[_root_.scala.Unit, $tpe](emptyRecord, _ => new $tpe(), _ => ())"
        Some(namedJsonSchemaTree(tpe, t))

      } else {

        val paramsTpes = paramsSeq.map { param =>
          if (param.returnTypeIn(tpe) <:< typeOf[Option[_]]) {
            param.returnTypeIn(tpe).typeArgs.head
          } else {
            param.returnTypeIn(tpe)
          }
        }

        val paramJsonSchemas = paramsTpes.map(findJsonSchema(_, allowImplicitSearch = true))
        if(!paramJsonSchemas.forall(_.isDefined)) {

          (paramsSeq zip paramsTpes zip paramJsonSchemas)
            .collect { case ((param, paramTpe), None) =>
              param.pos -> s"Can't derive JsonSchema[$paramTpe] required for field ${param.name}!"
            }
            .foreach { case (pos, err) => c.echo(pos, err) }

          None
        } else {

          val tupledRecordSchema = (paramsSeq zip paramJsonSchemas.flatten)
            .map { case (p, jsonSchema) =>
              if (p.returnTypeIn(tpe) <:< typeOf[Option[_]]) {
                q"optField[${p.returnTypeIn(tpe).typeArgs.head}](${p.name.decodedName.toString})($jsonSchema)"
              } else {
                q"field[${p.returnTypeIn(tpe)}](${p.name.decodedName.toString})($jsonSchema)"
              }
            }
            .reduce { (param1, param2) => q"zipRecords($param1, $param2)" }

          val paramNames = paramsSeq.map(p => c.freshName(p.name))
          val fPattern = paramNames.map(p => pq"$p @ _").reduce[Tree] { (p1, p2) => pq"($p1, $p2)" }

          val cc = Ident(TermName(c.freshName("cc")))
          val tp = paramsSeq.map(p => q"$cc.$p").reduce { (p1, p2) => q"($p1, $p2)" }

          val tupledTpe = paramsSeq.map(p => tq"${p.returnTypeIn(tpe)}").reduce[Tree] { (t1, t2) => tq"($t1, $t2)" }
          val recordSchemaTree =
            q"""
              invmapRecord[$tupledTpe, $tpe](
                $tupledRecordSchema,
                { case $fPattern => new $tpe(..${paramNames.map(Ident(_))}) },
                ($cc: $tpe) => $tp
              )
            """

          Some(namedJsonSchemaTree(tpe, recordSchemaTree))
        }
      }
    } else {
      None
    }
  }

  private def findTagged(tpe: Type): Option[c.Tree] = {

    if(tpe.typeSymbol.isSealedClass) {

      val instances = tpe.typeSymbol.classSymbolOpt.get.knownDirectSubclasses.toSeq.sortBy(_.name.decodedName.toString)

      instances.foreach(_.typeSignature)

      val instanceJsonSchema = instances.map { instance =>
        findJsonSchema(instance.tpe, allowImplicitSearch = true)
      }

      if(!instanceJsonSchema.forall(_.isDefined)) {

        (instances zip instanceJsonSchema)
          .collect { case (inst, None) =>
            inst.pos -> s"Can't derive JsonSchema[${inst.tpe}] required for coproduct type $tpe!"
          }
          .foreach { case (pos, err) => c.echo(pos, err) }

        None
      } else {
        val taggedRecords = (instances zip instanceJsonSchema.flatten).map { case (instance, jsonSchema) =>
          q"taggedRecord[${instance.tpe}]($jsonSchema.asInstanceOf[Record[${instance.tpe}]], ${instance.name.decodedName.toString})"
        }

        val n = taggedRecords.length

        val t = if(n == 1) {
          q"invmapTagged[${instances.head.tpe}, $tpe](${taggedRecords.head}, _root_.scala.Predef.identity, { case x: ${instances.head.tpe} => x})"
        } else {
          val allTagged = taggedRecords.reduce { (tagged1, tagged2) =>
            q"choiceTagged($tagged1, $tagged2)"
          }

          val fClauses = instances.zipWithIndex.map { case (instance, i) =>
            val name = TermName(c.freshName(instance.name.decodedName.toString))
            val initPat = if(i == 0) pq"$name @ _" else pq"_root_.scala.Right($name @ _)"
            val pat = applyN(n - i - 1, initPat)(p => pq"_root_.scala.Left($p)")
            cq"$pat => ${Ident(name)}"
          }

          val gClauses = instances.zipWithIndex.map { case (instance, i) =>
            val name = TermName(c.freshName(instance.name.decodedName.toString))
            val initTree = if(i == 0) q"${Ident(name)}" else pq"_root_.scala.Right(${Ident(name)})"
            val tree = applyN(n - i - 1, initTree)(t => q"_root_.scala.Left($t)")
            cq"$name : ${instance.tpe} => $tree"
          }

          val et = instances.map(i => tq"${i.tpe}").reduce[Tree] { case (t1, t2) =>
            tq"_root_.scala.Either[$t1, $t2]"
          }

          q"invmapTagged[$et, $tpe]($allTagged, { case ..$fClauses }, { case ..$gClauses })"
        }

        Some(namedJsonSchemaTree(tpe, t))
      }
    } else {
      None
    }
  }

  private def findImplicit(typeTree: c.Tree): Option[c.Tree] = {
    println(s"findImplicit($typeTree)")
    val tpeTree = c.typecheck(
      typeTree,
      silent = true,
      mode = c.TYPEmode,
      withImplicitViewsDisabled = true,
      withMacrosDisabled = true
    )

    scala.util
      .Try(c.inferImplicitValue(tpeTree.tpe, silent = true, withMacrosDisabled = true))
      .toOption
      .map { t => println(s"implicit found for $typeTree: $t"); t}
      .filterNot(_ == EmptyTree)
  }

  private def namedJsonSchemaTree(tpe: c.Type, jsonSchemaTree: c.Tree): c.Tree = {
    q"named($jsonSchemaTree, typeName(_root_.scala.Predef.implicitly[_root_.scala.reflect.ClassTag[$tpe]]))"
  }

  private def applyN[T](n: Int, x: T)(f: T => T): T = {
    if(n == 0) x else applyN(n - 1, f(x))(f)
  }
}
