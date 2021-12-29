package endpoints4s.algebra.client

import endpoints4s.algebra.ChunkedRequestEntitiesTestApi
import endpoints4s.algebra.ChunkedResponseEntitiesTestApi
import org.scalatest.EitherValues

trait ChunkedEntitiesTestSuite[
    T <: ChunkedRequestEntitiesTestApi with ChunkedResponseEntitiesTestApi
] extends ChunkedEntitiesRequestTestSuite[T]
    with ChunkedEntitiesResponseTestSuite[T]

trait ChunkedEntitiesRequestTestSuite[T <: ChunkedRequestEntitiesTestApi]
    extends StreamedRequestEndpointCalls[T] {
  import streamingClient.{uploadEndpointTest}

  "Encode chunks uploaded to a server" in {

    val expectedItems = List(
      Array[Byte](1),
      Array[Byte](2),
      Array[Byte](3),
      Array[Byte](4),
      Array[Byte](5),
      Array[Byte](6)
    )

    callStreamedEndpoint(uploadEndpointTest, expectedItems)
      .map(_ => succeed)
  }
}

trait ChunkedEntitiesResponseTestSuite[T <: ChunkedResponseEntitiesTestApi]
    extends StreamedResponseEndpointCalls[T]
    with EitherValues {
  import streamingClient.{streamedTextEndpointTest, streamedBytesEndpointTest}

  "Decode bytes chunks streamed by a server" in {

    val expectedItems = List(0.toByte, 1.toByte, 2.toByte)

    callStreamedEndpoint(streamedBytesEndpointTest, ())
      .map(res => res.map(_.value.toList).flatten shouldEqual expectedItems)
  }

  "Decode string chunks streamed by a server" in {

    val expectedItems = "aaabbbccc"

    callStreamedEndpoint(streamedTextEndpointTest, ()).map(res =>
      res.map(_.value).mkString shouldEqual expectedItems
    )
  }
}
