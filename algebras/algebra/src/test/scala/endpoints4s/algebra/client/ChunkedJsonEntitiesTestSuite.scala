package endpoints4s.algebra.client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import endpoints4s.algebra.ChunkedJsonEntitiesTestApi

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

trait ChunkedJsonEntitiesTestSuite[T <: ChunkedJsonEntitiesTestApi] extends ClientTestBase[T] {

  val streamingClient: T

  private implicit val serverSystem: ActorSystem = ActorSystem("server-system")
  val streamingPort: Int = findOpenPort

  def serving[A](route: Route)(thunk: => Unit): Unit = {
    whenReady(Http().newServerAt("localhost", streamingPort).bindFlow(route)) { binding =>
      try {
        thunk
      } finally {
        whenReady(binding.terminate(10.seconds))(_ => ())
      }
    }
  }

  import streamingClient.{Counter, Endpoint, Chunks, streamedEndpointTest, uploadEndpointTest}

  /** Calls the endpoint and accumulates the messages sent by the server.
    * (only endpoints streaming a finite number of items can be tested)
    */
  def callStreamedEndpoint[A, B](
      endpoint: Endpoint[A, Chunks[B]],
      req: A
  ): Future[Seq[Either[String, B]]]
  def callStreamedEndpoint[A, B](
      endpoint: Endpoint[Chunks[A], B],
      req: Source[A, _]
  ): Future[B]

  "Decode chunks streamed by a server" in {

    val expectedItems =
      Right(Counter(1)) :: Right(Counter(2)) :: Right(Counter(3)) :: Nil

    serving(
      (get & path("notifications")) {
        complete(
          HttpEntity.Chunked.fromData(
            ContentTypes.`application/json`,
            Source(
              List(
                ByteString("{\"value\":1}"),
                ByteString("{\"value\":2}"),
                ByteString("{\"value\":3}")
              )
            )
          )
        )
      }
    ) {
      whenReady(callStreamedEndpoint(streamedEndpointTest, ()))(
        _ shouldEqual expectedItems
      )
      ()
    }

  }

  "Report errors when decoding chunks streamed by a server" in {

    val expectedItems = Right(Counter(1)) :: Left(
      "java.lang.Throwable: DecodingFailure at .value: Int"
    ) :: Nil

    serving(
      (get & path("notifications")) {
        complete(
          HttpEntity.Chunked.fromData(
            ContentTypes.`application/json`,
            Source(
              List(ByteString("{\"value\":1}"), ByteString("{\"value\":true}"))
            )
          )
        )
      }
    ) {
      whenReady(callStreamedEndpoint(streamedEndpointTest, ()))(
        _ shouldEqual expectedItems
      )
      ()
    }

  }

  "Encode chunks uploaded to a server" in {

    val expectedItems = List(Array[Byte](1, 2, 3), Array[Byte](4, 5, 6))

    serving(
      (post & path("upload")) {
        extractRequestEntity { entity =>
          val bytes = entity.dataBytes.map(_.toArray)
          whenReady(bytes.runWith(Sink.seq))(_ shouldEqual expectedItems)
          complete(HttpEntity.Empty)
        }
      }
    ) {
      callStreamedEndpoint(uploadEndpointTest, Source(expectedItems))
      ()
    }
  }

}
