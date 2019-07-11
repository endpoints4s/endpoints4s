# Scala.js web client

Web client using `XMLHttpRequest`.

~~~ scala expandVars=true
"org.julienrf" %%% "endpoints-xhr-client" % "{{version}}"
~~~

[API documentation](api:endpoints.xhr.package)

## `Endpoints`

The `Endpoints` interpreter fixes the type `Endpoint[A, B]` to a function
from `A` to `Result[B]`, where `Result` is abstract and is intended
to be defined by more specialized interpreters.

An example of such an interpreter is `endpoints.xhr.client.future.Endpoints`,
which fixes the `Result[A]` type to `scala.concurrent.Future[A]`.

This means that, given the following endpoint definition:

~~~ scala src=../../../../../algebras/algebra/src/test/scala/endpoints/algebra/EndpointsDocs.scala#endpoint-definition
~~~

It can be invoked as follows:

~~~ scala src=../../../../../xhr/client/src/test/scala/endpoints/xhr/future/EndpointsDocs.scala#invocation
~~~

Another example is `endpoints.xhr.client.faithful.Endpoints`, which fixes
the `Result[A]` type to `faithful.Future[A]`. This interpreter requires
an additional dependency (which, in turn, depends on the
[faithful](https://github.com/julienrf/faithful) library):

~~~ scala expandVars=true
// Provides endpoints.xhr.client.faithful.Endpoints
"org.julienrf" %%% "endpoints-xhr-client-faithful" % "{{version}}"
~~~

## `Http1Streaming`

The `Http1Streaming` interpreter fixes the `ChunkedEndpoint[A, B]` type to a function from
`A` to a `ChunkedResponse[B]`, where
[ChunkedResponse](api:endpoints.xhr.Streaming$ChunkedResponse) is a type defined by
*endpoints* that wraps the underlying `XMLHttpRequest` object.

This means that, given the following endpoint definition:

~~~ scala src=../../../../../algebras/algebra/src/test/scala/endpoints/algebra/StreamingDocs.scala#chunked-endpoint
~~~

It can be invoked as follows:

~~~ scala src=../../../../../xhr/client/src/test/scala/endpoints/xhr/future/StreamingDocs.scala#invocation
~~~

---

The `WebSocketEndpoint[A, B, C]` type is fixed to a function from `A` to `WebSocket[B, C]`,
where [WebSocket](api:endpoints.xhr.Streaming$WebSocket) is a type defined by *endpoints*
that wraps the underlying DOM WebSocket object.

This means that, given the following WebSocket endpoint definition:

~~~ scala src=../../../../../algebras/algebra/src/test/scala/endpoints/algebra/StreamingDocs.scala#websocket-endpoint
~~~

It can be invoked as follows:

~~~ scala src=../../../../../xhr/client/src/test/scala/endpoints/xhr/future/StreamingDocs.scala#websocket-invocation
~~~
