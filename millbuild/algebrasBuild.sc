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
        override def ivyDeps = super.ivyDeps() ++ Agg(
          ivy"com.github.tomakehurst:wiremock:2.6.0"
        )
      }

    }

    class Jvm(val crossVersion: String) extends Module with EndpointsJvmModule {
      object test extends AlgebraTests with EndpointsJvmTests {
        override def moduleDeps = super.moduleDeps ++ Seq(jsonSchema.jvm(crossVersion).test)
      }
    }
    class Js(val crossVersion: String) extends Module with EndpointsJsModule {
      object test extends AlgebraTests with EndpointsJsTests {
        override def moduleDeps = super.moduleDeps ++ Seq(jsonSchema.js(crossVersion).test)
      }
    }

    object js extends Cross[Js](crossVersions: _*)
    object jvm extends Cross[Jvm](crossVersions: _*)
  }

//  object algebraCirce extends mill.Cross[AlgebraCirceModule](`scala 2.10 to 2.12`: _*)
//
//  object algebraPlayjson extends mill.Cross[AlgebraPlayjsonModule](`scala 2.10 to 2.12`: _*)
//
//  class AlgebraModule(val crossVersion: String) extends EndpointsModule {
//    override def artifactName = s"endpoints-algebra"
//
//    override def moduleDeps = Seq(jsonSchema(crossVersion))
//
//    object test extends Tests with EndpointsTests {
//      override def ivyDeps = super.ivyDeps() ++ Agg(
//        ivy"com.github.tomakehurst:wiremock:2.6.0"
//      )
//    }
//  }
//  class AlgebraJsModule(crossVersion: String) extends AlgebraModule(crossVersion) with EndpointsJsModule {
//    override def millSourcePath = super.millSourcePath / up / "algebra"
//
//    override def moduleDeps = Seq(jsonSchemaJs(crossVersion))
//
//    object test extends EndpointsJsTests {
//      override def ivyDeps = super.ivyDeps() ++ Agg(
//        ivy"com.github.tomakehurst::wiremock:2.6.0"
//      )
//    }
//  }
//
//  class AlgebraCirceModule(val crossVersion: String) extends EndpointsModule {
//    override def artifactName = s"endpoints-algebra-circe"
//
//    override def millSourcePath = super.millSourcePath / up / "algebra-circe"
//
//    override def ivyDeps = Agg(
//      ivy"io.circe::circe-parser::$circeVersion"
//    )
//
//    override def moduleDeps = Seq(algebra(crossVersion))
//
//    val test = new Tests with EndpointsTests {
//      override def moduleDeps = super.moduleDeps ++ Seq(algebra(crossVersion).test)
//
//      override def scalacPluginIvyDeps = super.scalacPluginIvyDeps() ++ Agg(
//        ivy"org.scalamacros:::paradise:2.1.0"
//      )
//
//      override def ivyDeps = super.ivyDeps() ++ Agg(
//        ivy"io.circe::circe-generic:$circeVersion"
//      )
//    }
//
//  }
//
//  class AlgebraPlayjsonModule(val crossVersion: String) extends EndpointsModule {
//    override def artifactName = s"endpoints-algebra-playjson"
//
//    override def millSourcePath = super.millSourcePath / up / "algebra-playjson"
//
//    override def ivyDeps = Agg(ivy"com.typesafe.play::play-json:$playVersion")
//
//    override def moduleDeps = Seq(algebra(crossVersion))
//
//    object test extends Tests with EndpointsTests {
//      override def moduleDeps = super.moduleDeps ++ Seq(algebra(crossVersion).test)
//    }
//
//  }

}
