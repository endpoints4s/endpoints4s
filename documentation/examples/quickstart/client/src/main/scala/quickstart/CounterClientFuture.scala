package quickstart

import endpoints4s.xhr

import scala.concurrent.Future

object CounterClientFuture
    extends CounterEndpoints
    with xhr.future.Endpoints
    with xhr.JsonEntitiesFromSchemas {

  //#endpoint-invocation
  val eventuallyDone: Future[Unit] = increment(Increment(step = 42))
  //#endpoint-invocation

}
