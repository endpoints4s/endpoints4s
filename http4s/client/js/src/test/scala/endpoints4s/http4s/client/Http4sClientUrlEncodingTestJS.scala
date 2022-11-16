package endpoints4s.http4s.client

import cats.effect.IO
import org.http4s.Uri
import org.http4s.dom.FetchClientBuilder

class Http4sClientUrlEncodingTestJS
    extends Http4sClientUrlEncodingTest[IO, TestJsonSchemaClient[IO]] {

  val client = new TestJsonSchemaClient[IO](
    Uri.Authority(
      host = Uri.RegName("localhost"),
      port = Some(stubServerPortHTTP)
    ),
    Uri.Scheme.http,
    FetchClientBuilder[IO].create
  )
}
