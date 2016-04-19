package endpoints

import cats.data.Xor
import io.circe.{Decoder, Encoder, Error, Json, jawn}
import play.api.http.{ContentTypes, Writeable}
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import play.api.mvc.Codec

import scala.concurrent.{ExecutionContext, Future}

class PlayClient(wsClient: WSClient)(implicit ec: ExecutionContext) extends EndpointsAlg {

  class Path[A](val apply: A => String) extends PathOps[A]

  def staticPathSegment(segment: String) = new Path((_: Unit) => segment)

  def dynamicPathSegment = new Path((s: String) => s)

  def chainedPaths[A, B](first: Path[A], second: Path[B])(implicit fc: FlatConcat[A, B]): Path[fc.Out] =
    new Path((ab: fc.Out) => {
      val (a, b) = fc.unapply(ab)
      first.apply(a) ++ "/" ++ second.apply(b)
    })


  type Request[A] = A => Future[WSResponse]

  type RequestEntity[A] = (A, WSRequest) => Future[WSResponse]

  type JsonRequest[A] = Encoder[A]

  def get[A](path: Path[A]) =
    a => wsClient.url(path.apply(a)).get()

  def post[A, B](path: Path[A], entity: RequestEntity[B])(implicit fc: FlatConcat[A, B]): Request[fc.Out] =
    (ab: fc.Out) => {
      val (a, b) = fc.unapply(ab)
      val wsRequest = wsClient.url(path.apply(a))
      entity(b, wsRequest)
    }

  implicit def jsonWriteable(implicit codec: Codec): Writeable[Json] =
    new Writeable[Json](json => codec.encode(json.noSpaces), Some(ContentTypes.JSON))

  def jsonRequest[A : JsonRequest] = {
    case (a, wsRequest) => wsRequest.post(Encoder[A].apply(a))
  }


  type Response[A] = WSResponse => Xor[Error, A]

  val emptyResponse: Response[Unit] = _ => Xor.Right(())

  type JsonResponse[A] = Decoder[A]

  def jsonResponse[A](implicit decoder: Decoder[A]) =
    response => jawn.parse(response.body).flatMap(decoder.decodeJson)


  type Endpoint[I, O] = I => Future[Xor[Error, O]]

  def endpoint[A, B](request: Request[A], response: Response[B]) =
    a => request(a).map(response)

}
