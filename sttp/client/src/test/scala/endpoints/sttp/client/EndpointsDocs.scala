package endpoints.sttp.client

import com.softwaremill.sttp.Id
import endpoints.algebra

trait EndpointsDocs extends Endpoints[Id] with algebra.EndpointsDocs {

  //#invocation
  val string: String = someResource(42)
  //#invocation

}
