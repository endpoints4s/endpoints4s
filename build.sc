// build files have to be place in single directory because of a bug in ammonite (`file` and `dir.^.file` are note the same files)
import $file.millbuild.common
import $file.millbuild.algebrasBuild
import $file.millbuild.openapiBuild
import $file.millbuild.akkahttp
import $file.millbuild.playBuild
import $file.millbuild.scalajBuild
import $file.millbuild.xhrBuild

import algebrasBuild.AlgebrasModule
import openapiBuild.OpenapiModule
import akkahttp.AkkaHttpModule
import playBuild.PlayModule
import scalajBuild.ScalajModule
import xhrBuild.XhrModule
import mill.eval.Evaluator

object algebras extends AlgebrasModule

object openapi extends OpenapiModule {
  override def algebra = algebras.algebra
  override def algebraCirce = algebras.algebraCirce
  override def jsonSchema = algebras.jsonSchema
}

object akkaHttp extends AkkaHttpModule {
  override def algebraJvm(crossVersion: String) = algebras.algebra.jvm(crossVersion)
  override def algebraCirceJvm(crossVersion: String) = algebras.algebraCirce.jvm(crossVersion)
}

object play extends PlayModule {
  override def algebraJvm(crossVersion: String) = algebras.algebra.jvm(crossVersion)
  override def algebraCirceJvm(crossVersion: String) = algebras.algebraCirce.jvm(crossVersion)
}

object scalaj extends ScalajModule {
  override def algebra(crossVersion: String) = algebras.algebra.jvm(crossVersion)
}

object xhr extends XhrModule {
  override def algebraJs(crossVersion: String) = algebras.algebra.js(crossVersion)
  override def algebraCirceJs(crossVersion: String) = algebras.algebraCirce.js(crossVersion)
}


def genidea(ev: Evaluator[Any]) = common.genideaImpl(ev)
