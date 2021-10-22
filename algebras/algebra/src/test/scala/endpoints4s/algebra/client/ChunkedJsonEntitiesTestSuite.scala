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

  import streamingClient.{
    Counter,
    Endpoint,
    Chunks,
    streamedEndpointTest,
    uploadEndpointTest,
    streamedTextEndpointTest,
    streamedBytesEndpointTest
  }

  /** Calls the endpoint and accumulates the messages sent by the server. (only endpoints streaming
    * a finite number of items can be tested)
    */
  def callStreamedEndpoint[A, B](
      endpoint: Endpoint[A, Chunks[B]],
      req: A
  ): Future[Seq[Either[String, B]]]
  def callStreamedEndpoint[A, B](
      endpoint: Endpoint[Chunks[A], B],
      req: Source[A, _]
  ): Future[B]

  "Decode bytes chunks streamed by a server" in {

    val expectedItems =
      Right(List(0.toByte)) :: Right(List(1.toByte)) :: Right(List(2.toByte)) :: Nil

    serving(
      (get & path("bytes")) {
        complete(
          HttpEntity.Chunked.fromData(
            ContentTypes.`application/octet-stream`,
            Source(
              List(
                ByteString(0.toByte),
                ByteString(1.toByte),
                ByteString(2.toByte)
              )
            )
          )
        )
      }
    ) {
      whenReady(callStreamedEndpoint(streamedBytesEndpointTest, ()))(res =>
        res.map(_.map(_.toList)) shouldEqual expectedItems
      )
      ()
    }

  }

  "Decode string chunks streamed by a server" in {

    val expectedItems =
      Right("aaa") :: Right("bbb") :: Right("ccc") :: Nil

    serving(
      (get & path("text")) {
        complete(
          HttpEntity.Chunked.fromData(
            ContentTypes.`text/plain(UTF-8)`,
            Source(
              List(
                ByteString("aaa"),
                ByteString("bbb"),
                ByteString("ccc")
              )
            )
          )
        )
      }
    ) {
      whenReady(callStreamedEndpoint(streamedTextEndpointTest, ()))(
        _ shouldEqual expectedItems
      )
      ()
    }

  }

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
          whenReady(bytes.runWith[Future[Seq[Array[Byte]]]](Sink.seq))(_ shouldEqual expectedItems)
          complete(HttpEntity.Empty)
        }
      }
    ) {
      callStreamedEndpoint(uploadEndpointTest, Source(expectedItems))
      ()
    }
  }

}
