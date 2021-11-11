# http4s

Client and server backed by [http4s](http://http4s.org).

## Client

@coordinates[http4s-client]

@scaladoc[API documentation](endpoints4s.http4s.client.index)

### `endpoints4s.http4s.client.Endpoints`

The `Endpoints` interpreter provides a trait `Endpoint[A, B]` defined as follows

@@snip [Endpoints.scala](/http4s/client/shared/src/main/scala/endpoints4s/http4s/client/Endpoints.scala) { #endpoint-type }

This means that, given the following endpoint definition:

@@snip [EndpointsDocs.scala](/algebras/algebra/src/test/scala/endpoints4s/algebra/EndpointsDocs.scala) { #endpoint-definition }

It can be invoked as follows using `IO`:

@@snip [EndpointsDocs.scala](/http4s/client/shared/src/test/scala/endpoints4s/http4s/client/EndpointsDocs.scala) { #invocation }

### `ChunkedEntities`

The `ChunkedEntities` interpreter fixes the `Chunks[A]` type to `fs2.Stream[Effect, A]`:

@@snip [ChunkedEntities.scala](/http4s/client/shared/src/main/scala/endpoints4s/http4s/client/ChunkedEntities.scala) { #stream-type }

This means that, given the following endpoint definition:

@@snip [ChunkedEntitiesDocs.scala](/algebras/algebra/src/test/scala/endpoints4s/algebra/ChunkedEntitiesDocs.scala) { #streamed-endpoint }

It can be invoked as follows:

@@snip [ChunkedEntitiesDocs.scala](/http4s/client/shared/src/test/scala/endpoints4s/http4s/client/ChunkedEntitiesDocs.scala) { #invocation }

## Server

@coordinates[http4s-server]

@scaladoc[API documentation](endpoints4s.http4s.server.index)

### `endpoints4s.http4s.server.Endpoints`

The `Endpoints` interpreter provides a `routesFromEndpoints` operation that
turns a sequence of endpoints with their implementation into an `org.http4s.HttpRoutes[F]`
value that can be integrated to your http4s application.

For instance, given the following endpoint definition:

@@snip [EndpointsDocs.scala](/algebras/algebra/src/test/scala/endpoints4s/algebra/EndpointsDocs.scala) { #endpoint-definition }

It can be implemented as follows:

@@snip [EndpointsDocs.scala](/http4s/server/src/test/scala/endpoints4s/http4s/server/EndpointsDocs.scala) { #implementation }

The result is a regular value of type `org.http4s.HttpRoute[IO]` that can be integrated in your application like
any other http4s service.

### Error handling

When the server processes requests, three kinds of errors can happen: the incoming request doesn’t match
any endpoint, the request does match an endpoint but is invalid (e.g. one parameter has a wrong type), or
an exception is thrown.

#### The incoming request doesn’t match any endpoint

In that case, the router constructed by endpoints4s can’t do anything. You have to deal with such
errors in the usual http4s way (usually, by adding a `.orNotFound` call to your application
services).

#### The incoming request is invalid

In that case, endpoints4s returns a “Bad Request” (400) response reporting all the errors in a
JSON array. You can change this behavior by overriding the
@scaladoc[handleClientErrors](endpoints4s.http4s.server.EndpointsWithCustomErrors) method.

#### An exception is thrown

If an exception is thrown during request decoding, or when running the business logic, or when
encoding the response, endpoints4s returns an “Internal Server Error” (500) response reporting
the error in a JSON array. You can change this behavior by overriding the
@scaladoc[handleServerError](endpoints4s.http4s.server.EndpointsWithCustomErrors) method.
