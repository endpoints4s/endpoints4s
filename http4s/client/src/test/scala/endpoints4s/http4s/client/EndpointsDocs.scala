package endpoints4s.http4s.client

import endpoints4s.algebra
import cats.effect.IO

trait EndpointsDocs extends Endpoints[IO] with algebra.EndpointsDocs {

  //#invocation
  val eventuallyString: IO[String] = someResource(42)
  //#invocation

}
