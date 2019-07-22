package quickstart

import endpoints.xhr

import scala.concurrent.Future

object CounterClientFuture
  extends CounterEndpoints
    with xhr.future.Endpoints
    with xhr.circe.JsonSchemaEntities {

  //#endpoint-invocation
  val eventuallyDone: Future[Unit] = increment(Increment(step = 42))
  //#endpoint-invocation

}

