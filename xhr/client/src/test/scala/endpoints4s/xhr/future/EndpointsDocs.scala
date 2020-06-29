package endpoints4s.xhr.future

import endpoints4s.algebra

import scala.concurrent.Future

trait EndpointsDocs extends algebra.EndpointsDocs with Endpoints {

  //#invocation
  val eventuallyString: Future[String] = someResource(42)
  //#invocation

}
