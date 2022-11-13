package endpoints4s.fetch.future

import endpoints4s.algebra
import endpoints4s.algebra.ChunkedJsonResponseEntitiesTestApi
import endpoints4s.algebra.ChunkedRequestEntitiesTestApi
import endpoints4s.algebra.ChunkedResponseEntitiesTestApi
import endpoints4s.algebra.circe.CounterCodecCirce
import endpoints4s.fetch.BasicAuthentication
import endpoints4s.fetch.ChunkedJsonResponseEntities
import endpoints4s.fetch.ChunkedRequestEntities
import endpoints4s.fetch.ChunkedResponseEntities
import endpoints4s.fetch.EndpointsSettings
import endpoints4s.fetch.JsonEntitiesFromCodecs
import org.scalajs.dom
import java.util.concurrent.TimeUnit

import endpoints4s.algebra.ChunkedJsonRequestEntitiesTestApi
import endpoints4s.fetch.ChunkedJsonRequestEntities

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.scalajs.concurrent.JSExecutionContext
import scala.scalajs.js
import scala.scalajs.js.Thenable.Implicits._
import scala.util.control.NonFatal

class TestClient(val settings: EndpointsSettings)
    extends Endpoints
    with BasicAuthentication
    with algebra.client.ClientEndpointsTestApi
    with algebra.BasicAuthenticationTestApi
    with algebra.TextEntitiesTestApi
    with algebra.JsonFromCodecTestApi
    with algebra.SumTypedEntitiesTestApi
    with algebra.circe.JsonFromCirceCodecTestApi
    with JsonEntitiesFromCodecs
    with ChunkedRequestEntities
    with ChunkedResponseEntities
    with ChunkedJsonRequestEntities
    with ChunkedJsonResponseEntities
    with algebra.circe.JsonEntitiesFromCodecs
    with ChunkedRequestEntitiesTestApi
    with ChunkedResponseEntitiesTestApi
    with ChunkedJsonRequestEntitiesTestApi
    with ChunkedJsonResponseEntitiesTestApi
    with CounterCodecCirce

class FetchClientEndpointsTest
    extends algebra.client.EndpointsTestSuite[TestClient]
    with algebra.client.BasicAuthTestSuite[TestClient]
    with algebra.client.JsonFromCodecTestSuite[TestClient]
    with algebra.client.TextEntitiesTestSuite[TestClient]
    with algebra.client.SumTypedEntitiesTestSuite[TestClient]
    with algebra.client.ChunkedEntitiesRequestTestSuite[TestClient]
    with algebra.client.ChunkedEntitiesResponseTestSuite[TestClient]
    with algebra.client.ChunkedJsonEntitiesRequestTestSuite[TestClient]
    with algebra.client.ChunkedJsonEntitiesResponseTestSuite[TestClient]
    with algebra.client.TimeoutTestSuite[TestClient] {

  implicit override def executionContext: ExecutionContextExecutor = JSExecutionContext.queue

  val baseUrl = s"https://localhost:$stubServerPort"

  val client: TestClient = new TestClient(
    EndpointsSettings()
      .withBaseUri(Some(baseUrl))
      .withTimeout(Some(FiniteDuration(2, TimeUnit.SECONDS)))
  )

  val streamingClient: TestClient = new TestClient(
    EndpointsSettings().withBaseUri(Some(baseUrl))
  )

  def call[Req, Resp](
      endpoint: client.Endpoint[Req, Resp],
      args: Req
  ): Future[Resp] = endpoint(args).future

  override def callStreamedEndpoint[A, B](
      endpoint: streamingClient.Endpoint[dom.ReadableStream[A], B],
      req: Seq[A]
  ): Future[B] = {
    endpoint(
      dom.ReadableStream[A](
        new dom.ReadableStreamUnderlyingSource[A] {
          start = js.defined((controller: dom.ReadableStreamController[A]) => {
            req.foreach(controller.enqueue)
            controller.close(): js.UndefOr[js.Promise[Unit]]
          }): js.UndefOr[
            js.Function1[dom.ReadableStreamController[A], js.UndefOr[js.Promise[Unit]]]
          ]
        }
      )
    ).future
  }

  override def callStreamedEndpoint[A, B](
      endpoint: streamingClient.Endpoint[A, dom.ReadableStream[B]],
      req: A
  ): Future[Seq[Either[String, B]]] = {
    endpoint(req).future
      .flatMap { readableStream =>
        def read(
            reader: dom.ReadableStreamReader[B]
        )(values: Seq[Either[String, B]]): Future[Seq[Either[String, B]]] = {
          reader
            .read()
            .flatMap { chunk =>
              if (chunk.done) {
                Future.successful(values)
              } else {
                read(reader)(values :+ Right(chunk.value))
              }
            }
            .recover { case NonFatal(e) =>
              values :+ Left(e.toString)
            }
        }
        read(readableStream.getReader())(Seq.empty)
      }
  }

  clientTestSuite()
  basicAuthSuite()
  jsonFromCodecTestSuite()
  textEntitiesTestSuite()
  sumTypedRequestsTestSuite()
}
