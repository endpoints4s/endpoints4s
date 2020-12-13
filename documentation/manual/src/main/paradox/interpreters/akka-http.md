# Akka HTTP

Client and server backed by [Akka HTTP](https://doc.akka.io/docs/akka-http/current/).

## Client

@coordinates[akka-http-client]

@scaladoc[API documentation](endpoints4s.akkahttp.client.index)

### `endpoints4s.akkahttp.client.Endpoints`

The `Endpoints` interpreter fixes the `Endpoint[A, B]` type to a function
from `A` to `Future[B]`:

@@snip [Endpoints.scala](/akka-http/client/src/main/scala/endpoints4s/akkahttp/client/Endpoints.scala) { #endpoint-type }

This means that, given the following endpoint definition:

@@snip [EndpointsDocs.scala](/algebras/algebra/src/test/scala/endpoints4s/algebra/EndpointsDocs.scala) { #endpoint-definition }

It can be invoked as follows:

@@snip [EndpointsDocs.scala](/akka-http/client/src/test/scala/endpoints4s/akkahttp/client/EndpointsDocs.scala) { #invocation }

### `endpoints4s.akkahttp.client.ChunkedEntities`

The `ChunkedEntities` interpreter fixes the `Chunks[A]` type to `akka.stream.scaladsl.Source[A, _]`:

@@snip [ChunkedEntities.scala](/akka-http/client/src/main/scala/endpoints4s/akkahttp/client/ChunkedEntities.scala) { #stream-type }

This means that, given the following endpoint definition:

@@snip [ChunkedEntitiesDocs.scala](/algebras/algebra/src/test/scala/endpoints4s/algebra/ChunkedEntitiesDocs.scala) { #streamed-endpoint }

It can be invoked as follows:

@@snip [ChunkedEntitiesDocs.scala](/akka-http/client/src/test/scala/endpoints4s/akkahttp/client/ChunkedEntitiesDocs.scala) { #invocation }

## Server

@coordinates[akka-http-server]

@scaladoc[API documentation](endpoints4s.akkahttp.server.index)

### `endpoints4s.akkahttp.server.Endpoints`

The `Endpoints` interpreter fixes the `Endpoint[A, B]` type to something that,
given an implementation function `A => B`, returns an `akka.http.scaladsl.server.Route`
that can be integrated to your Akka HTTP application.

For instance, given the following endpoint definition:

@@snip [EndpointsDocs.scala](/algebras/algebra/src/test/scala/endpoints4s/algebra/EndpointsDocs.scala) { #endpoint-definition }

It can be implemented as follows:

@@snip [EndpointsDocs.scala](/akka-http/server/src/test/scala/endpoints4s/akkahttp/server/EndpointsDocs.scala) { #implementation }

Alternatively, there is also a method `implementedByAsync` that takes an implementing function
returning a `Future[B]`.

### `endpoints4s.akkahttp.server.ChunkedEntities`

The `ChunkedEntities` interpreter fixes the `Chunks[A]` type to `akka.stream.scaladsl.Source[A, _]`.

For instance, given the following chunked endpoint definition:

@@snip [ChunkedEntitiesDocs.scala](/algebras/algebra/src/test/scala/endpoints4s/algebra/ChunkedEntitiesDocs.scala) { #streamed-endpoint }

It can be implemented as follows:

@@snip [ChunkedEntitiesDocs.scala](/akka-http/server/src/test/scala/endpoints4s/akkahttp/server/ChunkedEntitiesDocs.scala) { #implementation }

### Error handling

When the server processes requests, three kinds of errors can happen: the incoming request doesn’t match
any endpoint, the request does match an endpoint but is invalid (e.g. one parameter has a wrong type), or
an exception is thrown.

#### The incoming request doesn’t match any endpoint

In that case, the routes constructed by endpoints4s can’t do anything. You have to deal with such
errors in the usual Akka HTTP way: by using an implicit `akka.http.scaladsl.server.RejectionHandler`
having a `handleNotFound` clause.

#### The incoming request is invalid

In that case, endpoints4s returns a “Bad Request” (400) response reporting all the errors in a
JSON array. You can change this behavior by overriding the
@scaladoc[handleClientErrors](endpoints4s.akkahttp.server.Urls) method.

#### An exception is thrown

If an exception is thrown during request decoding, or when running the business logic, or when
encoding the response, endpoints4s returns an “Internal Server Error” (500) response reporting
the error in a JSON array. You can change this behavior by overriding the
@scaladoc[handleServerError](endpoints4s.akkahttp.server.Endpoints) method.
