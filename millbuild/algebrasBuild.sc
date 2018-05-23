import $file.common
import common.{EndpointsModule, EndpointsJsModule, EndpointsJvmModule, EndpointsGroupingModule, `scala 2.10 to 2.12`}

import mill._
import mill.scalalib._
import ammonite.ops._
import mill.define.Cross.Factory
import mill.define.{Cross, Discover}


trait AlgebrasModule  extends Module {

  object jsonSchema extends EndpointsGroupingModule{
    trait Module extends EndpointsModule {
      override def artifactName = s"endpoints-json-schema"
      override def millSourcePath = super.millSourcePath / up / "json-schema"
    }

    class Jvm(val crossVersion: String) extends Module with EndpointsJvmModule {
      object test extends EndpointsJvmTests
    }
    class Js(val crossVersion: String) extends Module with EndpointsJsModule {
      object test extends EndpointsJsTests
    }

    object js extends Cross[Js](crossVersions: _*)
    object jvm extends Cross[Jvm](crossVersions: _*)
  }

  object algebra extends EndpointsGroupingModule{
    trait Module extends EndpointsModule {
      override def artifactName = s"endpoints-algebra"
      override def millSourcePath = super.millSourcePath / up / "algebra"

      override def crossModuleDeps = Seq(jsonSchema)

      trait AlgebraTests extends EndpointsTests {
        override def ivyDeps = super.ivyDeps() ++ Agg(ivy"com.github.tomakehurst:wiremock:2.6.0")

        override def crossModuleDeps = Seq(jsonSchema)
      }

    }

    class Jvm(val crossVersion: String) extends Module with EndpointsJvmModule {
      object test extends AlgebraTests with EndpointsJvmTests
    }
    class Js(val crossVersion: String) extends Module with EndpointsJsModule {
      object test extends AlgebraTests with EndpointsJsTests
    }

    object js extends Cross[Js](crossVersions: _*)
    object jvm extends Cross[Jvm](crossVersions: _*)
  }

  object algebraCirce extends EndpointsGroupingModule{
    trait Module extends EndpointsModule {
      override def artifactName = s"endpoints-algebra-circe"
      override def millSourcePath = super.millSourcePath / up / "algebra-circe"

      override def crossModuleDeps = Seq(algebra)

      override def ivyDeps = super.ivyDeps() ++
        Agg(ivy"io.circe::circe-parser::$circeVersion")

      override def scalacPluginIvyDeps = super.scalacPluginIvyDeps() ++ Agg(ivy"org.scalamacros:::paradise:2.1.0")

      trait AlgebraCirceTest extends EndpointsTests {
        override def ivyDeps = super.ivyDeps() ++
          Agg(ivy"io.circe::circe-generic:$circeVersion")
        override def crossModuleDeps = Seq(algebra)
      }
    }

    class Jvm(val crossVersion: String) extends Module with EndpointsJvmModule {
      object test extends AlgebraCirceTest with EndpointsJvmTests
    }
    class Js(val crossVersion: String) extends Module with EndpointsJsModule {
      object test extends AlgebraCirceTest with EndpointsJsTests
    }

    object js extends Cross[Js](crossVersions: _*)
    object jvm extends Cross[Jvm](crossVersions: _*)
  }

  object algebraPlayjson extends EndpointsGroupingModule{
    trait Module extends EndpointsModule {
      override def artifactName = s"endpoints-algebra-playjson"
      override def millSourcePath = super.millSourcePath / up / "algebra-playjson"

      override def crossModuleDeps = Seq(algebra)

      override def ivyDeps = Agg(ivy"com.typesafe.play::play-json:$playVersion")
      trait AlgebraPlayjsonTest extends EndpointsTests {
        override def crossModuleDeps = Seq(algebra)
      }
    }

    class Jvm(val crossVersion: String) extends Module with EndpointsJvmModule {
      object test extends AlgebraPlayjsonTest with EndpointsJvmTests
    }
    class Js(val crossVersion: String) extends Module with EndpointsJsModule {
      object test extends AlgebraPlayjsonTest with EndpointsJsTests
    }

    object js extends Cross[Js](crossVersions: _*)
    object jvm extends Cross[Jvm](crossVersions: _*)
  }

}
