package endpoints.scalaj.client

import endpoints.algebra

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait EndpointsDocs extends algebra.EndpointsDocs with Endpoints {

  //#invocation
  val eventuallyString: Future[String] = someResource.callAsync(42)
  //#invocation

}
