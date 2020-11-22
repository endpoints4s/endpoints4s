package endpoints4s.sttp.client

import endpoints4s.algebra
import sttp.client.Identity

trait EndpointsDocs extends Endpoints[Identity] with algebra.EndpointsDocs {

  //#invocation
  val string: String = someResource(42)
  //#invocation

}
