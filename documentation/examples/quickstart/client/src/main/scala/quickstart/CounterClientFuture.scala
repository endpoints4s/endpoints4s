package quickstart

import endpoints4s.xhr
import endpoints4s.xhr.EndpointsSettings

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

object CounterClientFuture
    extends CounterEndpoints
    with xhr.future.Endpoints
    with xhr.JsonEntitiesFromSchemas {

  implicit val ec: ExecutionContext = ExecutionContext.global

  lazy val settings: EndpointsSettings = EndpointsSettings()

  //#endpoint-invocation
  val eventuallyDone: Future[Unit] = increment(Increment(step = 42)).future
  //#endpoint-invocation

}
