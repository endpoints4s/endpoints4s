package endpoints.http4s.client

import endpoints.algebra
import endpoints.algebra.client

import cats.effect.Sync
import org.http4s.client.Client
import cats.effect.IO
import cats.data.Kleisli
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.global
import org.http4s.client.asynchttpclient.AsyncHttpClient
import cats.effect.ContextShift
import endpoints.algebra.circe
import org.http4s.Uri
import akka.stream.scaladsl.Source
import akka.actor.ActorSystem
import streamz.converter._

class TestJsonSchemaClient[F[_]: Sync](host: Uri, client: Client[F])
    extends Endpoints[F](host, client)
    with BasicAuthentication
    with JsonEntitiesFromCodecs
    with ChunkedJsonEntities
    with algebra.BasicAuthenticationTestApi
    with algebra.EndpointsTestApi
    with algebra.JsonFromCodecTestApi
    with algebra.ChunkedJsonEntitiesTestApi
    with circe.JsonFromCirceCodecTestApi
    with circe.JsonEntitiesFromCodecs
    with algebra.circe.ChunkedJsonEntitiesTestApi

class Http4sClientEndpointsJsonSchemaTest
    extends client.EndpointsTestSuite[TestJsonSchemaClient[IO]]
    with client.BasicAuthTestSuite[TestJsonSchemaClient[IO]]
    with client.JsonFromCodecTestSuite[TestJsonSchemaClient[IO]]
    with client.ChunkedJsonEntitiesTestSuite[TestJsonSchemaClient[IO]] {

  implicit val system = ActorSystem()
  implicit val ctx: ContextShift[IO] = IO.contextShift(global)

  val (ahc, shutdown) =
    AsyncHttpClient.allocate[IO]().unsafeRunSync()

  val client = new TestJsonSchemaClient[IO](
    Uri.unsafeFromString(s"http://localhost:$wiremockPort"),
    ahc
  )

  def call[Req, Resp](
      endpoint: client.Endpoint[Req, Resp],
      args: Req
  ): Future[Resp] = endpoint(args).unsafeToFuture()

  def encodeUrl[A](url: client.Url[A])(a: A): String =
    url.encodeUrl(a).toOption.get.renderString

  def callStreamedEndpoint[A, B](
      endpoint: Kleisli[IO, fs2.Stream[IO, A], B],
      req: Source[A, _]
  ): Future[B] =
    endpoint.run(req.toStream[IO]()).unsafeToFuture()

  def callStreamedEndpoint[A, B](
      endpoint: Kleisli[IO, A, fs2.Stream[IO, B]],
      req: A
  ): Future[Seq[Either[String, B]]] =
    endpoint
      .run(req)
      .flatMap(stream =>
        stream.attempt.map(_.left.map(_.toString)).compile.toList
      )
      .unsafeToFuture()

  val streamingClient = new TestJsonSchemaClient[IO](
    Uri.unsafeFromString(s"http://localhost:$streamingPort"),
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
