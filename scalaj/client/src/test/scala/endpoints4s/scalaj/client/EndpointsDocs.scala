package endpoints4s.scalaj.client

import endpoints4s.algebra

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait EndpointDefinitions extends algebra.Endpoints {
  //#endpoint-definition
  val someResource: Endpoint[Int, String] =
    endpoint(get(path / "some-resource" / segment[Int]()), ok(textResponse))
  //#endpoint-definition
}

trait EndpointsDocs extends EndpointDefinitions with Endpoints {
  //#invocation
  val eventuallyString: Future[String] = someResource.callAsync(42)
  //#invocation
}
