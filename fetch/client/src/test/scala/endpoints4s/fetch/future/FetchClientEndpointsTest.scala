package endpoints4s.fetch.future

import akka.stream.scaladsl.Source
import endpoints4s.algebra
import endpoints4s.algebra.ChunkedJsonEntitiesTestApi
import endpoints4s.fetch.ChunkedJsonEntities
import endpoints4s.fetch.JsonEntitiesFromCodecs

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.util.control.NonFatal

class TestClient()(implicit
    val
    ec: ExecutionContext
) extends Endpoints
    with algebra.EndpointsTestApi
    with algebra.TextEntitiesTestApi
    with algebra.JsonFromCodecTestApi
    with algebra.SumTypedEntitiesTestApi
    with algebra.circe.JsonFromCirceCodecTestApi
    with JsonEntitiesFromCodecs
    with algebra.circe.JsonEntitiesFromCodecs
    with ChunkedJsonEntities
    with ChunkedJsonEntitiesTestApi
    with algebra.circe.ChunkedJsonEntitiesTestApi

//TODO needs Scala.js test setup
class FetchClientEndpointsTest
    extends algebra.client.EndpointsTestSuite[TestClient]
    with algebra.client.JsonFromCodecTestSuite[TestClient]
    with algebra.client.TextEntitiesTestSuite[TestClient]
    with algebra.client.SumTypedEntitiesTestSuite[TestClient]
    with algebra.client.ChunkedJsonEntitiesTestSuite[TestClient] {

  implicit val ec = ExecutionContext.global

  val client: TestClient = new TestClient()

  val streamingClient: TestClient = new TestClient()

  def call[Req, Resp](
      endpoint: client.Endpoint[Req, Resp],
      args: Req
  ): Future[Resp] = endpoint(args)

  def encodeUrl[A](url: client.Url[A])(a: A): String = url.encode(a)

  def callStreamedEndpoint[A, B](
      endpoint: streamingClient.Endpoint[A, streamingClient.Chunks[B]],
      req: A
  ): Future[Seq[Either[String, B]]] = {
    val result = mutable.Buffer[B]()
    val resultPromise = Promise[Seq[Either[String, B]]]()
    endpoint(req)
      .flatMap(_ { result += _ })
      .map(_ => resultPromise.completeWith(Future.successful(result.map(Right(_)).toSeq)))
      .recover { case NonFatal(ex) =>
        resultPromise.completeWith(Future.successful(Seq(Left(ex.toString))))
      }
    resultPromise.future
  }

  def callStreamedEndpoint[A, B](
      endpoint: streamingClient.Endpoint[streamingClient.Chunks[A], B],
      req: Source[A, _]
  ): Future[B] = ???

  clientTestSuite()
  jsonFromCodecTestSuite()
  textEntitiesTestSuite()
  sumTypedRequestsTestSuite()
}
