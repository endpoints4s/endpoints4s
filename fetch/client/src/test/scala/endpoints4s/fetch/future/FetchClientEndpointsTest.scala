package endpoints4s.fetch.future

import endpoints4s.algebra
import endpoints4s.algebra.ChunkedJsonResponseEntitiesTestApi
import endpoints4s.algebra.ChunkedResponseEntitiesTestApi
import endpoints4s.algebra.circe.CounterCodecCirce
import endpoints4s.fetch.BasicAuthentication
import endpoints4s.fetch.ChunkedJsonResponseEntities
import endpoints4s.fetch.ChunkedResponseEntities
import endpoints4s.fetch.EndpointsSettings
import endpoints4s.fetch.JsonEntitiesFromCodecs
import org.scalajs.dom

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext
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
    with ChunkedResponseEntities
    with ChunkedJsonResponseEntities
    with algebra.circe.JsonEntitiesFromCodecs
    with ChunkedResponseEntitiesTestApi
    with ChunkedJsonResponseEntitiesTestApi
    with CounterCodecCirce

class FetchClientEndpointsTest
    extends algebra.client.EndpointsTestSuite[TestClient]
    with algebra.client.BasicAuthTestSuite[TestClient]
    with algebra.client.JsonFromCodecTestSuite[TestClient]
    with algebra.client.TextEntitiesTestSuite[TestClient]
    with algebra.client.SumTypedEntitiesTestSuite[TestClient]
    with algebra.client.ChunkedEntitiesResponseTestSuite[TestClient]
    with algebra.client.ChunkedJsonEntitiesResponseTestSuite[TestClient] {

  implicit override def executionContext: ExecutionContextExecutor = JSExecutionContext.queue

  val client: TestClient = new TestClient(
    EndpointsSettings().withBaseUri(Some("http://localhost:8080"))
  )

  def call[Req, Resp](
      endpoint: client.Endpoint[Req, Resp],
      args: Req
  ): Future[Resp] = endpoint(args).future

  override def callStreamedEndpoint[A, B](
      endpoint: streamingClient.Endpoint[A, dom.ReadableStream[B]],
      req: A
  ): Future[Seq[Either[String, B]]] = {
    endpoint(req)
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

  val streamingClient: TestClient = new TestClient(
    EndpointsSettings().withBaseUri(Some("http://localhost:8080"))
  )

  clientTestSuite()
  basicAuthSuite()
  jsonFromCodecTestSuite()
  textEntitiesTestSuite()
  sumTypedRequestsTestSuite()
}
