import mill._
import mill.scalalib._
import mill.scalalib.publish._
import ammonite.ops.up
import mill.define.Cross.Factory
import mill.define.{Cross, Discover, ExternalModule, Target}
import mill.eval.Evaluator
import mill.scalajslib._

// THIS FILE HAS TO BE ON THE SAME LEVEL AS FILES USING IT BECAUSE OF A BUG IN AMMONITE


//val `scala 2.11 to 2.12` = Seq("2.12.4")
val `scala 2.11 to 2.12` = Seq("2.11.12", "2.12.4")

trait EndpointsModule extends SbtModule with PublishModule {
  def crossVersion: String

  def scalaVersion = crossVersion

  def publishVersion = "0.0.2"

  def pomSettings = PomSettings(
    description = "description",
    organization = "org.julienrf",
    url = "http://julienrf.github.io/endpoints/",
    licenses = Seq(
      License("MIT license", "http://www.opensource.org/licenses/mit-license.php")
    ),
    scm = SCM(
      "git://github.com:julienrf/endpoints.git",
      "scm:git://github.com:julienrf/endpoints.git"
    ),
    developers = Seq(
      Developer("julienrf", "Julien Richard-Foy", "http://julien.richard-foy.fr")
    )
  )

  val circeVersion = "0.8.0"
  val playVersion = "2.6.7"

  override def millSourcePath = super.millSourcePath / up

  trait EndpointsTests extends Tests {

    override def ivyDeps = super.ivyDeps() ++ Agg(ivy"org.scalatest::scalatest::3.0.4")

    def testFrameworks = Seq("org.scalatest.tools.Framework")

    def crossModuleDeps: Seq[EndpointsGroupingModule] = Seq()
  }

  def crossModuleDeps: Seq[EndpointsGroupingModule] = Seq()

  override def scalacOptions = super.scalacOptions() ++ Seq(
    "-feature",
    "-deprecation",
    "-encoding", "UTF-8",
    "-unchecked",
    "-Xlint",
    "-Yno-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard",
    "-Xfuture",
    "-Xexperimental"
  )

}

trait EndpointsJsModule extends EndpointsModule with ScalaJSModule {
  def scalaJSVersion = "0.6.22"

  trait EndpointsJsTests extends EndpointsTests with Tests {
    override def moduleDeps = super.moduleDeps ++ crossModuleDeps.map(_.js(crossVersion).test)
  }

  def test: EndpointsJsTests

  override def moduleDeps: Seq[PublishModule] =
    super.moduleDeps ++ this.crossModuleDeps.map(_.js(crossVersion))

}

trait EndpointsJvmModule extends EndpointsModule {

  trait EndpointsJvmTests extends EndpointsTests with Tests {
    override def moduleDeps = super.moduleDeps ++ crossModuleDeps.map(_.jvm(crossVersion).test)
  }

  def test: EndpointsJvmTests

  override def moduleDeps: Seq[PublishModule] =
    super.moduleDeps ++ this.crossModuleDeps.map(_.jvm(crossVersion))
}

trait EndpointsGroupingModule extends Module {
  override def millSourcePath = super.millSourcePath / up

  def crossVersions: Seq[String] = `scala 2.11 to 2.12`

  type Js <: EndpointsJsModule
  type Jvm <: EndpointsJvmModule

  def js: Cross[Js]

  def jvm: Cross[Jvm]
}


def genideaImpl(ev: Evaluator[Any]) = T.command {
  import ammonite.ops._
  mill.scalalib.GenIdeaImpl(
    implicitly,
    ev.rootModule,
    ev.rootModule.millDiscover
  )
  // to have proper highlghting in intellij each project needs to be rooted in different directory. so we move all tests
  // from module to module/src/test. It does not affect anything in practice.
  val files = (ls ! pwd / ".idea_modules")
    .filter(_.toString().endsWith("test.iml"))
  files.foreach { file =>
    val content = read(file)
    val regex = """<content url="file://\$MODULE_DIR\$/\.\./(.*)">""".r
    val replacement = """<content url="file://\$MODULE_DIR\$/\.\./$1/src/test">"""
    val newContent = regex.replaceFirstIn(content, replacement)
    write.over(file, newContent)
  }
}
