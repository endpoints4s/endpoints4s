package endpoints4s.algebra.client

import endpoints4s.algebra.ChunkedJsonRequestEntitiesTestApi
import endpoints4s.algebra.ChunkedJsonResponseEntitiesTestApi

trait ChunkedJsonEntitiesTestSuite[
    T <: ChunkedJsonRequestEntitiesTestApi with ChunkedJsonResponseEntitiesTestApi
] extends ChunkedJsonEntitiesRequestTestSuite[T]
    with ChunkedJsonEntitiesResponseTestSuite[T]

trait ChunkedJsonEntitiesRequestTestSuite[T <: ChunkedJsonRequestEntitiesTestApi]
    extends StreamedRequestEndpointCalls[T] {
  import streamingClient.{Counter, streamedJsonUpload}

  "Encode chunks streamed to server" in {
    val expectedItems =
      Counter(1) :: Counter(2) :: Counter(3) :: Nil

    callStreamedEndpoint(streamedJsonUpload, expectedItems)
      .map(_ => succeed)
  }
}

trait ChunkedJsonEntitiesResponseTestSuite[T <: ChunkedJsonResponseEntitiesTestApi]
    extends StreamedResponseEndpointCalls[T] {
  import streamingClient.{
    Counter,
    streamedEndpointTest,
    streamedEndpointErrorTest,
    streamedEndpointEmptyTest
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

  "Succeed if server streams no chunks" in {

    val expectedItems = List.empty

    callStreamedEndpoint(streamedEndpointEmptyTest, ())
      .map(_ shouldEqual expectedItems)
  }
}
