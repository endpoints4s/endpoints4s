package endpoints4s.play.client

import endpoints4s.algebra

import scala.concurrent.Future

trait EndpointsDocs extends Endpoints with algebra.EndpointsDocs {

  //#invocation
  val eventuallyString: Future[String] = someResource(42)
  //#invocation

}
