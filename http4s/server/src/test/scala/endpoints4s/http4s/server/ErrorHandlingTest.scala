package endpoints4s.http4s.server

import akka.http.scaladsl.model.HttpMethods.PUT
import akka.http.scaladsl.model.HttpRequest
import cats.effect.IO
import endpoints4s.algebra
import org.http4s

import java.util.UUID

class ErrorHandlingTest extends Http4sServerTest[Endpoints[IO]] {

  val serverApi = new Endpoints[IO]
    with algebra.EndpointsTestApi {

    private val magicValue = UUID.randomUUID().toString

    // Pretend that we could not decode the incoming request
    override def emptyRequest: http4s.Request[IO] => IO[Either[http4s.Response[IO], Unit]] =
      _ => IO.raiseError(new RuntimeException(magicValue))

    // Transform the error when the request could not be decoded
    override def handleServerError(request: http4s.Request[IO], throwable: Throwable): IO[http4s.Response[IO]] = {
      if (throwable.getMessage == magicValue)
        IO.pure(http4s.Response(http4s.Status.PaymentRequired))
      else
        super.handleServerError(request, throwable)
    }

  }

  "Server" should {
    "call the hook handleServerError when a request fails to match the endpoints" in {
      serveEndpoint(serverApi.putEndpoint, ()) { port =>
        val request =
          HttpRequest(method = PUT, uri = s"http://localhost:$port/user/foo123")
        whenReady(send(request)) { case (response, _) =>
          assert(response.status.intValue() == 402)
        }
        ()
      }
    }
  }

}
