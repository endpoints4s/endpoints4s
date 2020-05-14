package endpoints.http4s.client

import endpoints.algebra
import endpoints.algebra.client

import cats.effect.Sync
import org.http4s.client.Client
import cats.effect.IO
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.global
import org.http4s.client.asynchttpclient.AsyncHttpClient
import cats.effect.ContextShift
import endpoints.algebra.circe
import org.http4s.Uri

class TestJsonSchemaClient[F[_]: Sync](host: Uri, client: Client[F])
    extends Endpoints[F](host, client)
    with BasicAuthentication
    with JsonEntitiesFromCodecs
    with algebra.BasicAuthenticationTestApi
    with algebra.EndpointsTestApi
    with algebra.JsonFromCodecTestApi
    with circe.JsonFromCirceCodecTestApi
    with circe.JsonEntitiesFromCodecs

class Http4sClientEndpointsJsonSchemaTest
    extends client.EndpointsTestSuite[TestJsonSchemaClient[IO]]
    with client.BasicAuthTestSuite[TestJsonSchemaClient[IO]]
    with client.JsonFromCodecTestSuite[TestJsonSchemaClient[IO]] {

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
  ): Future[Resp] = {
    Thread.sleep(50)
    val eventualResponse = endpoint(args)
    Thread.sleep(50)
    eventualResponse.unsafeToFuture()
  }

  def encodeUrl[A](url: client.Url[A])(a: A): String =
    url.encodeUrl(a).toOption.get.renderString

  clientTestSuite()
  basicAuthSuite()
  jsonFromCodecTestSuite()

  override def afterAll(): Unit = {
    shutdown.unsafeRunSync()
    super.afterAll()
  }

}
