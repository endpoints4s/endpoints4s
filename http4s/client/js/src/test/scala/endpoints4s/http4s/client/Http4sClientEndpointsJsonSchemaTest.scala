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

import scala.concurrent.Future

class TestJsonSchemaClient[F[_]: Concurrent](
    authority: Uri.Authority,
    scheme: Uri.Scheme,
    client: Client[F]
) extends Endpoints[F](authority, scheme, client)
    with BasicAuthentication
    with JsonEntitiesFromCodecs
    with algebra.BasicAuthenticationTestApi
    with algebra.client.ClientEndpointsTestApi
    with algebra.JsonFromCodecTestApi
    with algebra.SumTypedEntitiesTestApi
    with circe.JsonFromCirceCodecTestApi
    with circe.JsonEntitiesFromCodecs

class Http4sClientEndpointsJsonSchemaTest
    extends client.EndpointsTestSuite[TestJsonSchemaClient[IO]]
    with client.BasicAuthTestSuite[TestJsonSchemaClient[IO]]
    with client.JsonFromCodecTestSuite[TestJsonSchemaClient[IO]]
    with client.SumTypedEntitiesTestSuite[TestJsonSchemaClient[IO]] {

  type EffectResource[A] = effect.Resource[IO, A]

  val client = new TestJsonSchemaClient[IO](
    Uri.Authority(
      host = Uri.RegName("localhost"),
      port = Some(stubServerPort)
    ),
    Uri.Scheme.http,
    FetchClientBuilder[IO].create
  )

  def call[Req, Resp](
      endpoint: client.Endpoint[Req, Resp],
      args: Req
  ): Future[Resp] = {
    val eventualResponse = endpoint.sendAndConsume(args)
    eventualResponse.unsafeToFuture()
  }

  clientTestSuite()
  basicAuthSuite()
  jsonFromCodecTestSuite()
  sumTypedRequestsTestSuite()

}
