package endpoints4s.http4s.client

import endpoints4s.algebra
import cats.effect.IO

trait EndpointDefinitions extends algebra.Endpoints {
  //#endpoint-definition
  val someResource: Endpoint[Int, String] =
    endpoint(get(path / "some-resource" / segment[Int]()), ok(textResponse))
  //#endpoint-definition
}

trait EndpointsDocs extends Endpoints[IO] with EndpointDefinitions {
  //#invocation
  val eventuallyString: IO[String] = someResource.sendAndConsume(42)
  //#invocation
}
