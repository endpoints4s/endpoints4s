package sample

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import scala.io.StdIn

object Server {

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("my-system")
    implicit val executionContext = system.dispatcher

    val bindingFuture = Http().newServerAt("localhost", 8080).bind(Api.routes)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine()
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }
}
