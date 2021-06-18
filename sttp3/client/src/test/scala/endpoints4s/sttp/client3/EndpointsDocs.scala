package endpoints4s.sttp.client3

import endpoints4s.algebra
import sttp.client3.Identity

trait EndpointsDocs extends Endpoints[Identity] with algebra.EndpointsDocs {

  //#invocation
  val string: String = someResource(42)
  //#invocation

}
