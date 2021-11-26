package endpoints4s.algebra.client

import endpoints4s.algebra.ChunkedJsonEntitiesTestApi

import scala.concurrent.Future

protected trait ChunkedJsonEntitiesTestSuite[T <: ChunkedJsonEntitiesTestApi]
    extends ClientTestBase[T] {
  val streamingClient: T

  import streamingClient.{Endpoint, Chunks}

  /** Calls the endpoint and accumulates the messages sent by the server.
    * (only endpoints streaming a finite number of items can be tested)
    */
  def callStreamedEndpoint[A, B](
      endpoint: Endpoint[A, Chunks[B]],
      req: A
  ): Future[Seq[Either[String, B]]]
  def callStreamedEndpoint[A, B](
      endpoint: Endpoint[Chunks[A], B],
      req: Seq[A]
  ): Future[B]
}

trait ChunkedJsonEntitiesResponseTestSuite[T <: ChunkedJsonEntitiesTestApi]
    extends ChunkedJsonEntitiesTestSuite[T] {
  import streamingClient.{
    Counter,
    streamedEndpointTest,
    streamedEndpointErrorTest,
    streamedTextEndpointTest,
    streamedBytesEndpointTest
  }

  "Decode bytes chunks streamed by a server" in {

    val expectedItems =
      Right(List(0.toByte)) :: Right(List(1.toByte)) :: Right(List(2.toByte)) :: Nil

    callStreamedEndpoint(streamedBytesEndpointTest, ())
      .map(res => res.map(_.map(_.toList)) shouldEqual expectedItems)
  }

  "Decode string chunks streamed by a server" in {

    val expectedItems =
      Right("aaa") :: Right("bbb") :: Right("ccc") :: Nil

    callStreamedEndpoint(streamedTextEndpointTest, ()).map(_ shouldEqual expectedItems)
  }

  "Decode chunks streamed by a server" in {

    val expectedItems =
      Right(Counter(1)) :: Right(Counter(2)) :: Right(Counter(3)) :: Nil

    callStreamedEndpoint(streamedEndpointTest, ())
      .map(_ shouldEqual expectedItems)
  }

  "Report errors when decoding chunks streamed by a server" in {

    val expectedItems = Right(Counter(1)) :: Left(
      "java.lang.Throwable: DecodingFailure at .value: Int"
    ) :: Nil

    callStreamedEndpoint(streamedEndpointErrorTest, ())
      .map(_ shouldEqual expectedItems)
  }
}

trait ChunkedJsonEntitiesRequestTestSuite[T <: ChunkedJsonEntitiesTestApi]
    extends ChunkedJsonEntitiesTestSuite[T] {
  import streamingClient.{uploadEndpointTest}

  "Encode chunks uploaded to a server" in {

    val expectedItems = List(Array[Byte](1, 2, 3), Array[Byte](4, 5, 6))

    callStreamedEndpoint(uploadEndpointTest, expectedItems)
      .map(_ => succeed)
  }
}
