package endpoints4s.http4s.client

import cats.effect
import cats.effect.Concurrent
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import endpoints4s.algebra
import endpoints4s.algebra.circe
import endpoints4s.algebra.client
import org.http4s.Uri
import org.http4s.client.Client
import org.http4s.dom.FetchClientBuilder

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.concurrent.JSExecutionContext

class TestJsonSchemaClient[F[_]: Concurrent](
    authority: Uri.Authority,
    scheme: Uri.Scheme,
    client: Client[F]
) extends Endpoints[F](authority, scheme, client)
    with BasicAuthentication
    with JsonEntitiesFromCodecs
    with ChunkedJsonEntities
    with algebra.BasicAuthenticationTestApi
    with algebra.client.ClientEndpointsTestApi
    with algebra.JsonFromCodecTestApi
    with algebra.SumTypedEntitiesTestApi
    with algebra.ChunkedEntitiesTestApi
    with algebra.ChunkedJsonEntitiesTestApi
    with circe.JsonFromCirceCodecTestApi
    with circe.JsonEntitiesFromCodecs
    with algebra.circe.CounterCodecCirce

class Http4sClientEndpointsJsonSchemaTest
    extends client.EndpointsTestSuite[TestJsonSchemaClient[IO]]
    with client.BasicAuthTestSuite[TestJsonSchemaClient[IO]]
    with client.JsonFromCodecTestSuite[TestJsonSchemaClient[IO]]
    with client.SumTypedEntitiesTestSuite[TestJsonSchemaClient[IO]]
    with client.ChunkedEntitiesTestSuite[TestJsonSchemaClient[IO]]
    with client.ChunkedJsonEntitiesTestSuite[TestJsonSchemaClient[IO]]
    with client.TimeoutTestSuite[TestJsonSchemaClient[IO]] {

  type EffectResource[A] = effect.Resource[IO, A]

  val client = new TestJsonSchemaClient[IO](
    Uri.Authority(
      host = Uri.RegName("localhost"),
      port = Some(stubServerPort)
    ),
    Uri.Scheme.http,
    FetchClientBuilder[IO].withRequestTimeout(FiniteDuration(2, TimeUnit.SECONDS)).create
  )

  def call[Req, Resp](
      endpoint: client.Endpoint[Req, Resp],
      args: Req
  ): Future[Resp] = {
    val eventualResponse = endpoint.sendAndConsume(args)
    eventualResponse.unsafeToFuture()
  }

  override def callStreamedEndpoint[A, B](
      endpoint: streamingClient.Endpoint[fs2.Stream[IO, A], B],
      req: Seq[A]
  ): Future[B] =
    endpoint
      .send(fs2.Stream.emits(req))
      .use(res => IO.pure(res))
      .unsafeToFuture()

  override def callStreamedEndpoint[A, B](
      endpoint: streamingClient.Endpoint[A, fs2.Stream[IO, B]],
      req: A
  ): Future[Seq[Either[String, B]]] =
    endpoint
      .send(req)
      .use(stream => stream.attempt.map(_.left.map(_.toString)).compile.toList)
      .unsafeToFuture()

  override val streamingClient = new TestJsonSchemaClient[IO](
    Uri.Authority(
      host = Uri.RegName("localhost"),
      port = Some(stubServerPort)
    ),
    Uri.Scheme.http,
    FetchClientBuilder[IO].create
  )

  implicit override def executionContext: ExecutionContext = JSExecutionContext.queue

  clientTestSuite()
  basicAuthSuite()
  jsonFromCodecTestSuite()
  sumTypedRequestsTestSuite()

}
