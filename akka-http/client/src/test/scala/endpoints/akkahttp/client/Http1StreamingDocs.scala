package endpoints.akkahttp.client

import endpoints.algebra

trait Http1StreamingDocs extends algebra.Http1StreamingDocs with Http1Streaming { this: Endpoints =>

  //#invocation
  import akka.stream.scaladsl.Source

  val bytesSource: Source[Array[Byte], _] = logo(())

  bytesSource.runForeach { bytes =>
    println(s"Received ${bytes.length} bytes")
  }
  //#invocation

  //#websocket-invocation
  import akka.Done
  import akka.stream.scaladsl.Flow

  // A participant that sends back in upper case any received message
  val repeatLouder = Flow.fromFunction { message: String =>
    println(s"Received: $message")
    val response = message.toUpperCase
    println(s"Sending: $response")
    response
  }

  val (eventualJoined, _)  = chat((), repeatLouder)

  eventualJoined.foreach {
    case Some(Done) => println("Joined the chat room")
    case None       => println("Unable to join the chat room")
  }
  //#websocket-invocation

}
