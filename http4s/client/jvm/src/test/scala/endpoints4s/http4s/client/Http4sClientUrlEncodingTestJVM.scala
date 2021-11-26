package endpoints4s.http4s.client

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.http4s.Uri
import org.http4s.asynchttpclient.client.AsyncHttpClient
import org.scalatest.BeforeAndAfterAll

class Http4sClientUrlEncodingTestJVM
    extends Http4sClientUrlEncodingTest[IO, TestJsonSchemaClient[IO]]
    with BeforeAndAfterAll {

  val (ahc, shutdown) =
    AsyncHttpClient.allocate[IO]().unsafeRunSync()

  val client = new TestJsonSchemaClient[IO](
    Uri.Authority(
      host = Uri.RegName("localhost"),
      port = Some(8080)
    ),
    Uri.Scheme.http,
    ahc
  )

  override def afterAll(): Unit = {
    shutdown.unsafeRunSync()
    super.afterAll()
  }
}
