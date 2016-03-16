package julienrf.endpoints

import cats.data.Xor
import io.circe.{Error, Encoder, Decoder, jawn}
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext

class PlayClient(wsClient: WSClient)(implicit ec: ExecutionContext) extends Endpoints {

  type Path[A] = A => String

  def static(segment: String) = _ => segment

  def dynamic = (s: String) => s

  def chained[A, B](first: Path[A], second: Path[B])(implicit fc: FlatConcat[A, B]): Path[fc.Out] =
    (ab: fc.Out) => {
      val (a, b) = fc.unapply(ab)
      first(a) ++ "/" ++ second(b)
    }


  type Request[A] = A => WSRequest

  type RequestMarshaller[A] = Encoder[A]

  def get[A](path: Path[A]) =
    a => wsClient.url("/" ++ path(a)) // TODO Use an intermediate data type holding the HTTP verb


  type Response[A] = WSResponse => Xor[Error, A]

  type ResponseMarshaller[A] = Decoder[A]

  def jsonEntity[A](implicit decoder: Decoder[A]) =
    response => jawn.parse(response.body).flatMap(decoder.decodeJson)


  type Endpoint[I, O] = I => Future[Xor[Error, O]]

  def endpoint[A, B](request: Request[A], response: Response[B]) =
    a => request(a).get().map(response)

}
