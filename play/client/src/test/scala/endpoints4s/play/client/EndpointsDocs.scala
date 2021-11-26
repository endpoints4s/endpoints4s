package endpoints4s.play.client

import endpoints4s.algebra

import scala.concurrent.Future

trait EndpointDefinitions extends algebra.Endpoints {
  //#endpoint-definition
  val someResource: Endpoint[Int, String] =
    endpoint(get(path / "some-resource" / segment[Int]()), ok(textResponse))
  //#endpoint-definition
}

trait EndpointsDocs extends Endpoints with EndpointDefinitions {
  //#invocation
  val eventuallyString: Future[String] = someResource(42)
  //#invocation
}
