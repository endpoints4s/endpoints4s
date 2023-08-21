package endpoints4s.pekkohttp.client

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.model.{HttpEntity, HttpRequest, HttpResponse, Uri}
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.{Flow, Sink, Source}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Try

final case class EndpointsSettings(
                                    requestExecutor: PekkoHttpRequestExecutor,
                                    baseUri: Uri = Uri("/"),
                                    toStrictTimeout: FiniteDuration = 2.seconds,
                                    stringContentExtractor: HttpEntity.Strict => String = _.data.utf8String
)

trait PekkoHttpRequestExecutor {
  def apply(request: HttpRequest): Future[HttpResponse]
}

object PekkoHttpRequestExecutor {
  def cachedHostConnectionPool(host: String, port: Int)(implicit
      system: ActorSystem,
      materializer: Materializer
  ): PekkoHttpRequestExecutor =
    default(Http().cachedHostConnectionPool[Int](host, port))

  def default(
      poolClientFlow: Flow[
        (HttpRequest, Int),
        (Try[HttpResponse], Int),
        Http.HostConnectionPool
      ]
  )(implicit materializer: Materializer): PekkoHttpRequestExecutor =
    new PekkoHttpRequestExecutor {
      override def apply(request: HttpRequest): Future[HttpResponse] =
        Source
          .single(request -> 1)
          .via(poolClientFlow)
          .map(_._1.get)
          .runWith(Sink.head)
    }
}
