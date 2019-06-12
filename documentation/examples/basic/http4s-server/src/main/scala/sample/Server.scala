package sample

import cats.effect._
import cats.implicits._
import org.http4s.server.blaze._
import org.http4s.implicits._

//#app
object Server extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    BlazeServerBuilder[IO]
      .bindHttp(8080, "localhost")
      .withHttpApp(Api.router.orNotFound)
      .resource
      .use(_ => IO.never)
      .as(ExitCode.Success)
}
//#app
