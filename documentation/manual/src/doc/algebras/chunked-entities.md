# Chunked Entities

## `ChunkedEntities`

This algebra provides vocabulary to describe endpoints whose requests or
responses are streamed using the “chunked transfer-encoding” supported by HTTP1.1.

~~~ scala expandVars=true
"org.julienrf" %% "endpoints-algebra" % "{{version}}"
~~~

[API documentation](unchecked:/api/endpoints/algebra/ChunkedEntities.html)

The `ChunkedEntities` module enriches the `Endpoints` algebra with operations for defining
request and responses entities carrying stream of values.

For instance, you can define an endpoint streaming a binary file as follows:

~~~ scala src=../../../../../algebras/algebra/src/test/scala/endpoints/algebra/ChunkedEntitiesDocs.scala#streamed-endpoint
~~~

The return type, `Endpoint[Unit, Chunks[Array[Byte]]]`, represents an endpoint whose request
takes no parameter (`Unit`) and whose response produces a stream of `Array[Byte]` chunks.

Responses are streamed using the
[chunked transfer-encoding](https://en.wikipedia.org/wiki/Chunked_transfer_encoding),
which is supported by most HTTP 1.1 clients and servers.

## `ChunkedJsonEntities`

~~~ scala expandVars=true
"org.julienrf" %% "endpoints-algebra" % "{{version}}"
~~~

[API documentation](unchecked:/api/endpoints/algebra/ChunkedJsonEntities.html)

Clients and servers have to agree on the serialization format used by response
chunks and WebSocket messages. The `ChunkedJsonEntities` module provides a `jsonChunksRequest`
constructor and a `jsonChunksResponse` constructor for defining request entities and response
entities carrying streams of values that are serialized into JSON:

~~~ scala src=../../../../../algebras/algebra/src/test/scala/endpoints/algebra/ChunkedEntitiesDocs.scala#json-streaming
~~~

This example uses the `JsonEntitiesFromSchemas` algebra to derive the JSON serialization
format from a JSON schema, which can also be reused by the OpenAPI interpreter.

Eventually, mix a `JsonEntitiesFromSchemas` interpreter of your choice to turn the JSON
schemas into proper JSON codecs. For instance, for Akka-Http:

~~~ scala src=../../../../../akka-http/server/src/test/scala/endpoints/akkahttp/server/ChunkedEntitiesDocs.scala#json-streaming
~~~

## Custom Serialization Format

Support for other serialization formats can be added by defining an operation
returning a `Chunks[A]` value. For instance, the
[Protocol Buffers](https://developers.google.com/protocol-buffers) format would be
supported as follows (using [protoless](https://julien-lafont.github.io/protoless/)):

~~~ scala
import endpoints.algebra
import io.protoless.messages.{Decoder, Encoder}

trait ProtobufChunkedEntities extends algebra.ChunkedEntities {
  /** Streams containing values of type `A`, serialized with the given protobuf codec */
  def protobufChunksRequest[A : Encoder : Decoder]: RequestEntity[Chunks[A]]
}
~~~

And then, the `protobufChunksRequest` operation would have to be implemented on each interpreter.
