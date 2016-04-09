package julienrf.endpoints

import cats.data.Xor
import io.circe.{Json, Error, Encoder, Decoder, jawn}
import play.api.http.{ContentTypes, Writeable}
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext

class PlayClient(wsClient: WSClient)(implicit ec: ExecutionContext) extends Endpoints {

//  type Path[A] = A => String
  class Path[A](val apply: A => String) extends PathOps[A]

  def static(segment: String) = new Path((_: Unit) => segment)

  def dynamic = new Path((s: String) => s)

  def chained[A, B](first: Path[A], second: Path[B])(implicit fc: FlatConcat[A, B]): Path[fc.Out] =
    new Path((ab: fc.Out) => {
      val (a, b) = fc.unapply(ab)
      first.apply(a) ++ "/" ++ second.apply(b)
    })


  type Request[A] = A => Future[WSResponse]

  type RequestEntity[A] = (A, WSRequest) => Future[WSResponse]

  type RequestMarshaller[A] = Encoder[A]

  def get[A](path: Path[A]) =
    a => wsClient.url(path.apply(a)).get()

  def post[A, B](path: Path[A], entity: RequestEntity[B])(implicit fc: FlatConcat[A, B]): Request[fc.Out] =
    (ab: fc.Out) => {
      val (a, b) = fc.unapply(ab)
      val wsRequest = wsClient.url(path.apply(a))
      entity(b, wsRequest)
    }

  object request extends RequestApi {
    implicit val jsonWriteable: Writeable[Json] =
      new Writeable[Json](_.noSpaces.getBytes("UTF-8"), Some(ContentTypes.JSON))
    def jsonEntity[A : RequestMarshaller] = {
      case (a, wsRequest) => wsRequest.post(Encoder[A].apply(a))
    }
  }


  type Response[A] = WSResponse => Xor[Error, A]

  type ResponseMarshaller[A] = Decoder[A]

  def jsonEntity[A](implicit decoder: Decoder[A]) =
    response => jawn.parse(response.body).flatMap(decoder.decodeJson)


  type Endpoint[I, O] = I => Future[Xor[Error, O]]

  def endpoint[A, B](request: Request[A], response: Response[B]) =
    a => request(a).map(response)

}
