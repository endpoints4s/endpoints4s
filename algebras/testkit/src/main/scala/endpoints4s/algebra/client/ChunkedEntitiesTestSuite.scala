package endpoints4s.algebra.client

import endpoints4s.algebra.ChunkedRequestEntitiesTestApi
import endpoints4s.algebra.ChunkedResponseEntitiesTestApi

trait ChunkedEntitiesTestSuite[
    T <: ChunkedRequestEntitiesTestApi with ChunkedResponseEntitiesTestApi
] extends ChunkedEntitiesRequestTestSuite[T]
    with ChunkedEntitiesResponseTestSuite[T]

trait ChunkedEntitiesRequestTestSuite[T <: ChunkedRequestEntitiesTestApi]
    extends StreamedEndpointCalls[T] {
  import streamingClient.{uploadEndpointTest}

  "Encode chunks uploaded to a server" in {

    val expectedItems = List(Array[Byte](1, 2, 3), Array[Byte](4, 5, 6))

    callStreamedEndpoint(uploadEndpointTest, expectedItems)
      .map(_ => succeed)
  }
}

trait ChunkedEntitiesResponseTestSuite[T <: ChunkedResponseEntitiesTestApi]
    extends StreamedEndpointCalls[T] {
  import streamingClient.{streamedTextEndpointTest, streamedBytesEndpointTest}

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
}
