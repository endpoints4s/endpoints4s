package sample

import cats.effect._
import org.http4s.server.blaze._
import org.http4s.implicits._

import scala.concurrent.ExecutionContext

//#app
object Server extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    BlazeServerBuilder[IO](ExecutionContext.global)
      .bindHttp(8080, "localhost")
      .withHttpApp(Api.router.orNotFound)
      .resource
      .use(_ => IO.never)
      .as(ExitCode.Success)
}
//#app
