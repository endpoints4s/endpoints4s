package endpoints.xhr.future

import endpoints.algebra

import scala.concurrent.Future

trait EndpointsDocs extends algebra.EndpointsDocs with Endpoints {

  //#invocation
  val eventuallyString: Future[String] = someResource(42)
  //#invocation

}
