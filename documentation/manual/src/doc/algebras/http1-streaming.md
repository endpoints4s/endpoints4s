# HTTP1.1 Streaming

## `Http1Streaming`

This algebra provides vocabulary to describe WebSockets and endpoints whose
responses are streamed using the “chunked transfer-encoding” supported by HTTP1.1.

~~~ scala expandVars=true
"org.julienrf" %% "endpoints-algebra" % "{{version}}"
~~~

[API documentation](api:endpoints.algebra.Http1Streaming)

### Chunked Endpoints

The `Http1Streaming` module enriches the `Endpoints` algebra with an operation for defining
endpoints whose responses produce a stream of values.

For instance, you can define an endpoint streaming a binary file as follows:

~~~ scala src=../../../../../algebras/algebra/src/test/scala/endpoints/algebra/Http1StreamingDocs.scala#chunked-endpoint
~~~

The return type, `ChunkedEndpoint[Unit, Array[Byte]]`, type means an endpoint whose request
takes no parameter (`Unit`) and whose response produces a stream of `Array[Byte]` values.

Conceptually, the type `ChunkedEndpoint[A, B]` is equivalent to `Endpoint[A, Chunks[B]]`,
where the type `Chunks[A]` means a stream of values of type `A`. Please refer to the
documentation of your interpreter to know the concrete semantic type given to
`ChunkedEndpoint`.

Responses are streamed using the
[chunked transfer-encoding](https://en.wikipedia.org/wiki/Chunked_transfer_encoding),
which is supported by most HTTP 1.1 clients and servers.

### WebSockets

Bidirectional communication is possible via
[WebSockets](https://en.wikipedia.org/wiki/WebSocket). For instance, here is how
you can define a WebSocket endpoint for joining a chat room:

~~~ scala src=../../../../../algebras/algebra/src/test/scala/endpoints/algebra/Http1StreamingDocs.scala#websocket-endpoint
~~~

The return type, `WebSocketEndpoint[Unit, String, String]`, means a WebSocket endpoint
whose request takes no parameter, and which yields a channel receiving `String` messages
and sending `String` messages.

## `Http1JsonStreaming`

~~~ scala expandVars=true
"org.julienrf" %% "endpoints-algebra" % "{{version}}"
~~~

[API documentation](api:endpoints.algebra.Http1JsonStreaming)

Clients and servers have to agree on the serialization format used by response
chunks and WebSocket messages. The `Http1JsonStreaming` module provides a `jsonChunks`
constructor for defining streams of values that are serialized into the JSON text
format:

~~~ scala src=../../../../../documentation/examples/basic/shared/src/main/scala/sample/JsonStreamingExample.scala#json-streaming
~~~

This example uses the `JsonSchemaEntities` algebra to derive the JSON serialization
format from a JSON schema, which can also be reused by the OpenAPI interpreter.

Eventually, mix a `JsonSchemaEntities` interpreter of your choice to turn the JSON
schemas into proper JSON codecs. For instance, for Akka-Http and using Circe:

~~~ scala src=../../../../../documentation/examples/basic/akkahttp-server/src/main/scala/sample/JsonStreamingExampleServer.scala#json-streaming
~~~

## Custom Serialization Format

Support for other serialization formats can be added by defining an operation
returning a `Chunks[A]` value. For instance, the
[Protocol Buffers](https://developers.google.com/protocol-buffers) format would be
supported as follows (using [protoless](https://julien-lafont.github.io/protoless/)):

~~~ scala
import endpoints.algebra
import io.protoless.messages.{Decoder, Encoder}

trait ProtobufStreaming extends algebra.Http1Streaming {

  /** Chunks containing values of type `A`, serialized with the given protobuf codec */
  def protobufChunks[A : Encoder : Decoder]: Chunks[A] { type Serialized = Array[Byte] }

}
~~~

And then, the `protobufChunks` operation would have to be implemented on each interpreter.
