// build files have to be place in single directory because of a bug in ammonite (`file` and `dir.^.file` are note the same files)
import $file.millbuild.common
import $file.millbuild.akkahttp
import $file.millbuild.algebrasBuild
import $file.millbuild.openapiBuild
import $file.millbuild.playBuild
import $file.millbuild.scalajBuild

import common.{EndpointsModule, `scala 2.10 to 2.12`}
import akkahttp.AkkaHttpModule
import algebrasBuild.AlgebrasModule
import openapiBuild.OpenapiModule
import playBuild.PlayModule
import scalajBuild.ScalajModule
import mill.eval.Evaluator

object algebras extends AlgebrasModule {
  override def jsonSchema(crossVersion: String) = openapi.jsonSchema(crossVersion)
}

object openapi extends OpenapiModule {
  override def algebra(crossVersion: String) = algebras.algebra(crossVersion)
}

object akkaHttp extends AkkaHttpModule {
  override def algebra(crossVersion: String) = algebras.algebra(crossVersion)
}

object play extends PlayModule {
  override def algebra(crossVersion: String) = algebras.algebra(crossVersion)
}

object scalaj extends ScalajModule {
  override def algebra(crossVersion: String) = algebras.algebra(crossVersion)
}

def genidea(ev: Evaluator[Any]) = common.genideaImpl(ev)
