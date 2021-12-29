package endpoints4s.algebra.client

import endpoints4s.algebra.Chunks
import endpoints4s.algebra.Endpoints

import scala.concurrent.Future

trait StreamedRequestEndpointCalls[T <: Endpoints with Chunks] extends ClientTestBase[T] {
  val streamingClient: T

  import streamingClient.{Endpoint, Chunks}

  /** Calls the endpoint and accumulates the messages sent by the server.
    * (only endpoints streaming a finite number of items can be tested)
    */
  def callStreamedEndpoint[A, B](
      endpoint: Endpoint[Chunks[A], B],
      req: Seq[A]
  ): Future[B]
}

trait StreamedResponseEndpointCalls[T <: Endpoints with Chunks] extends ClientTestBase[T] {
  val streamingClient: T

  import streamingClient.{Endpoint, Chunks}

  /** Calls the endpoint and accumulates the messages sent by the server.
    * (only endpoints streaming a finite number of items can be tested)
    */
  def callStreamedEndpoint[A, B](
      endpoint: Endpoint[A, Chunks[B]],
      req: A
  ): Future[Seq[Either[String, B]]]
}
