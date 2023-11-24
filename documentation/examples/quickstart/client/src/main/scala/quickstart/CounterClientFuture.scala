package quickstart

import endpoints4s.fetch
import endpoints4s.fetch.EndpointsSettings

import scala.concurrent.Future

object CounterClientFuture
    extends CounterEndpoints
    with fetch.future.Endpoints
    with fetch.JsonEntitiesFromSchemas {

  lazy val settings: EndpointsSettings = EndpointsSettings()

  //#endpoint-invocation
  val eventuallyDone: Future[Unit] = increment(Increment(step = 42)).future
  //#endpoint-invocation

}
