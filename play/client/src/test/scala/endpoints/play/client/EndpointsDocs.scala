package endpoints.play.client

import endpoints.algebra

import scala.concurrent.Future

trait EndpointsDocs extends Endpoints with algebra.EndpointsDocs {

  //#invocation
  val eventuallyString: Future[String] = someResource(42)
  //#invocation

}
