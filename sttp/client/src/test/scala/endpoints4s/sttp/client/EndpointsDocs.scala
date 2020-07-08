package endpoints4s.sttp.client

import com.softwaremill.sttp.Id
import endpoints4s.algebra

trait EndpointsDocs extends Endpoints[Id] with algebra.EndpointsDocs {

  //#invocation
  val string: String = someResource(42)
  //#invocation

}
