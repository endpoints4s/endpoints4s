import sbtcrossproject.CrossProject
import sbtcrossproject.CrossPlugin.autoImport._
import scalajscrossproject.ScalaJSCrossPlugin.autoImport._

import sbt.{ClasspathDependency, LocalProject}

object LocalCrossProject {

  implicit class LocalCrossProjectOps(crossProject: CrossProject) {

    def dependsOnLocalCrossProjects(names: String*): CrossProject =
      crossProject
        .jsConfigure(_.dependsOn(names.map(name => LocalProject(s"${name}JS"): ClasspathDependency): _*))
        .jvmConfigure(_.dependsOn(names.map(name => LocalProject(s"${name}JVM"): ClasspathDependency): _*))

    def dependsOnLocalCrossProjectsWithScope(namesAndScopes: (String, String)*): CrossProject =
      crossProject
        .jsConfigure(_.dependsOn(namesAndScopes.map{ case (name, scope) => LocalProject(s"${name}JS") % scope: ClasspathDependency}: _*))
        .jvmConfigure(_.dependsOn(namesAndScopes.map{ case (name, scope) => LocalProject(s"${name}JVM") % scope: ClasspathDependency}: _*))

  }

}
