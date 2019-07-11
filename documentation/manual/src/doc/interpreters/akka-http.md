# Akka HTTP

Client and server backed by [Akka HTTP](https://doc.akka.io/docs/akka-http/current/).

## Client

~~~ scala expandVars=true
"org.julienrf" %% "endpoints-akka-http-client" % "{{version}}"
~~~

[API documentation](api:endpoints.akkahttp.client.package)

### `Endpoints`

The `Endpoints` interpreter fixes the `Endpoint[A, B]` type to a function
from `A` to `Future[B]`:

~~~ scala src=../../../../../akka-http/client/src/main/scala/endpoints/akkahttp/client/Endpoints.scala#endpoint-type
~~~

This means that, given the following endpoint definition:

~~~ scala src=../../../../../algebras/algebra/src/test/scala/endpoints/algebra/EndpointsDocs.scala#endpoint-definition
~~~

It can be invoked as follows:

~~~ scala src=../../../../../akka-http/client/src/test/scala/endpoints/akkahttp/client/EndpointsDocs.scala#invocation
~~~

### `Http1Streaming`

The `Http1Streaming` interpreter fixes the `ChunkedEndpoint[A, B]` type to a function from
`A` to a `Source[B, _]`:

~~~ scala src=../../../../../akka-http/client/src/main/scala/endpoints/akkahttp/client/Http1Streaming.scala#chunked-endpoint-type
~~~

This means that, given the following endpoint definition:

~~~ scala src=../../../../../algebras/algebra/src/test/scala/endpoints/algebra/StreamingDocs.scala#chunked-endpoint
~~~

It can be invoked as follows:

~~~ scala src=../../../../../akka-http/client/src/test/scala/endpoints/akkahttp/client/StreamingDocs.scala#invocation
~~~

---

The `WebSocketEndpoint[A, B, C]` type is fixed to a function taking as parameters an `A`
and a `Flow[C, B, Mat]`, for some materializer type `Mat`. It returns a pair containing a
`Future[Option[Done]]`, which is eventually completed when the connection is established, and
the `Mat` materialized value:

~~~ scala src=../../../../../akka-http/client/src/main/scala/endpoints/akkahttp/client/Http1Streaming.scala#websocket-endpoint-type
~~~

This means that, given the following WebSocket endpoint definition:

~~~ scala src=../../../../../algebras/algebra/src/test/scala/endpoints/algebra/StreamingDocs.scala#websocket-endpoint
~~~

It can be invoked as follows:

~~~ scala src=../../../../../akka-http/client/src/test/scala/endpoints/akkahttp/client/StreamingDocs.scala#websocket-invocation
~~~

In this example we ignore the `NotUsed` value materialized by the `repeatLouder` flow.

## Server

~~~ scala expandVars=true
"org.julienrf" %% "endpoints-akka-http-server" % "{{version}}"
~~~

[API documentation](api:endpoints.akkahttp.server.package)

### `Endpoints`

The `Endpoints` interpreter fixes the `Endpoint[A, B]` type to a class that has an
`implementedBy` method that takes an implementation function `A => B` and returns an
`akka.http.scaladsl.server.Route` that can be integrated to your Akka HTTP application.

For instance, given the following endpoint definition:

~~~ scala src=../../../../../algebras/algebra/src/test/scala/endpoints/algebra/EndpointsDocs.scala#endpoint-definition
~~~

It can be implemented as follows:

~~~ scala src=../../../../../akka-http/server/src/test/scala/endpoints/akkahttp/server/EndpointsDocs.scala#implementation
~~~

Alternatively, there is an `implementedByAsync` that takes an implementing function returning
a `Future[B]`.

### `Http1Streaming`

The `Http1Streaming` interpreter fixes the `ChunkedEndpoint[A, B]` type to a class that has an
`implementedBy` method that takes a function `A => Source[B, _]` and returns a `Route`.
For instance, given the following chunked endpoint definition:

~~~ scala src=../../../../../algebras/algebra/src/test/scala/endpoints/algebra/StreamingDocs.scala#chunked-endpoint
~~~

It can be implemented as follows:

~~~ scala src=../../../../../akka-http/server/src/test/scala/endpoints/akkahttp/server/StreamingDocs.scala#implementation
~~~

---

Similarly, the `WebSocketEndpoint[A, B, C]` type has an `implementedBy` method, which
takes a function `A => Future[Option[Flow[B, C, _]]]` and returns a `Route`.
For instance, given the following WebSocket endpoint definition:

~~~ scala src=../../../../../algebras/algebra/src/test/scala/endpoints/algebra/StreamingDocs.scala#websocket-endpoint
~~~

It can be implemented as follows:

~~~ scala src=../../../../../akka-http/server/src/test/scala/endpoints/akkahttp/server/StreamingDocs.scala#websocket-implementation
~~~

In this example, the server always reply to user messages with the “I agree” response.

The implementing function can return a `None` value to signal a failure to open the
WebSocket.
