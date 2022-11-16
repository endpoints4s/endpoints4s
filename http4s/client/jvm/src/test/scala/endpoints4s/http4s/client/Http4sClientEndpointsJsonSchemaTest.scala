package endpoints4s.http4s.client

import endpoints4s.algebra
import endpoints4s.algebra.client

import cats.effect.Concurrent
import org.http4s.client.Client
import cats.effect.IO
import scala.concurrent.Future

import cats.effect

import _root_.org.http4s.asynchttpclient.client.AsyncHttpClient
import endpoints4s.algebra.circe
import org.http4s.Uri

import cats.effect.unsafe.implicits.global

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
    with client.ChunkedJsonEntitiesTestSuite[TestJsonSchemaClient[IO]] {

  type EffectResource[A] = effect.Resource[IO, A]

  val (ahc, shutdown) =
    AsyncHttpClient.allocate[IO]().unsafeRunSync()

  val client = new TestJsonSchemaClient[IO](
    Uri.Authority(
      host = Uri.RegName("localhost"),
      port = Some(stubServerPortHTTP)
    ),
    Uri.Scheme.http,
    ahc
  )

  def call[Req, Resp](
      endpoint: client.Endpoint[Req, Resp],
      args: Req
  ): Future[Resp] = {
    Thread.sleep(50)
    val eventualResponse = endpoint.sendAndConsume(args)
    Thread.sleep(50)
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
      port = Some(stubServerPortHTTP)
    ),
    Uri.Scheme.http,
    ahc
  )

  clientTestSuite()
  basicAuthSuite()
  jsonFromCodecTestSuite()

  override def afterAll(): Unit = {
    shutdown.unsafeRunSync()
    super.afterAll()
  }

}
