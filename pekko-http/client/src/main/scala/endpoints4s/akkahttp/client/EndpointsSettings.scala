package endpoints4s.akkahttp.client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpEntity, HttpRequest, HttpResponse, Uri}
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Try

final case class EndpointsSettings(
    requestExecutor: AkkaHttpRequestExecutor,
    baseUri: Uri = Uri("/"),
    toStrictTimeout: FiniteDuration = 2.seconds,
    stringContentExtractor: HttpEntity.Strict => String = _.data.utf8String
)

trait AkkaHttpRequestExecutor {
  def apply(request: HttpRequest): Future[HttpResponse]
}

object AkkaHttpRequestExecutor {
  def cachedHostConnectionPool(host: String, port: Int)(implicit
      system: ActorSystem,
      materializer: Materializer
  ): AkkaHttpRequestExecutor =
    default(Http().cachedHostConnectionPool[Int](host, port))

  def default(
      poolClientFlow: Flow[
        (HttpRequest, Int),
        (Try[HttpResponse], Int),
        Http.HostConnectionPool
      ]
  )(implicit materializer: Materializer): AkkaHttpRequestExecutor =
    new AkkaHttpRequestExecutor {
      override def apply(request: HttpRequest): Future[HttpResponse] =
        Source
          .single(request -> 1)
          .via(poolClientFlow)
          .map(_._1.get)
          .runWith(Sink.head)
    }
}
