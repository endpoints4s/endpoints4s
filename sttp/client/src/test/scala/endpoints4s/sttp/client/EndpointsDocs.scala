package endpoints4s.sttp.client

import endpoints4s.algebra
import sttp.client3.Identity

trait EndpointDefinitions extends algebra.Endpoints {
  //#endpoint-definition
  val someResource: Endpoint[Int, String] =
  endpoint(get(path / "some-resource" / segment[Int]()), ok(textResponse))
  //#endpoint-definition
}

trait EndpointsDocs extends Endpoints[Identity] with EndpointDefinitions {
  //#invocation
  val string: String = someResource(42)
  //#invocation
}
