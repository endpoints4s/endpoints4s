package sample

import cats.effect.IO
import endpoints.http4s.server.Endpoints
import org.http4s.HttpRoutes


object Api extends Endpoints[IO] {
  /**
    *
    * val helloWorldService = HttpRoutes.of[IO] {
    *   case GET -> Root / "hello" / name =>
    *     Ok(s"Hello, $name.")
    * }
    *
    */
  val maybe =
    endpoint(get(path / "random" / "result"), wheneverFound(textResponse()))

  val textToText =
    endpoint(post(path/ "text" / segment[Int]() / "text" /? (qs[Long]("param") & qs[Long]("param")) , textRequest()), textResponse())

  val router: HttpRoutes[IO] = HttpRoutes.of(
    maybe.implementedBy  { _ =>
      if (util.Random.nextBoolean()) Some("random") else None
    } orElse
    textToText.implementedBy {
      case ((intSegment, param1, param2), text) =>
        s"Modified: $text with segment $intSegment and query params $param1 and $param2"
    }
  )

//  val helloWorldService = HttpRoutes.of[IO] {
//    case req @ GET -> Root / "hello" / name =>
//         Ok(s"Hello, $name.")
//     }
}
