# Play framework

Client and server backed by [Play framework](https://www.playframework.com/).

## Client

@coordinates[play-client]

@scaladoc[API documentation](endpoints4s.play.client.index)

The `Endpoints` interpreter fixes the `Endpoint[A, B]` type to a function from
`A` to `Future[B]`:

@@snip [Endpoints.scala](/play/client/src/main/scala/endpoints4s/play/client/Endpoints.scala) { #concrete-carrier-type }

This means that, given the following endpoint definition:

@@snip [EndpointsDocs.scala](/play/client/src/test/scala/endpoints4s/play/client/EndpointsDocs.scala) { #endpoint-definition }

It can be invoked as follows:

@@snip [EndpointsDocs.scala](/play/client/src/test/scala/endpoints4s/play/client/EndpointsDocs.scala) { #invocation }

## Server

@coordinates[play-server]

@scaladoc[API documentation](endpoints4s.play.server.index)

### `Endpoints`

The `Endpoints` interpreter provides a `routesFromEndpoints` operation that turns
a sequence of endpoints with their implementation into a `play.api.routing.Router.Routes`
value that can be integrated to your Play application.

For instance, given the following endpoint definition:

@@snip [EndpointsDocs.scala](/play/server/src/test/scala/endpoints4s/play/server/EndpointsDocs.scala) { #endpoint-definition }

It can be implemented as follows:

@@snip [EndpointsDocs.scala](/play/server/src/test/scala/endpoints4s/play/server/EndpointsDocs.scala) { #implementation }

In practice, the routes are put in a class taking an `endpoints4s.play.server.PlayComponents`
parameter. An HTTP server can then be started as in the following example:

@@snip [Counter.scala](/documentation/examples/documented/src/main/scala/counter/Counter.scala) { #main-only }

### `ChunkedEntities`

The `ChunkedEntities` interpreter fixes the type `Chunks[A]` to `akka.stream.scaladsl.Source[A, _]`.

For instance, given the following chunked endpoint definition:

@@snip [ChunkedEntitiesDocs.scala](/play/server/src/test/scala/endpoints4s/play/server/ChunkedEntitiesDocs.scala) { #streamed-endpoint }

It can be implemented as follows:

@@snip [ChunkedEntitiesDocs.scala](/play/server/src/test/scala/endpoints4s/play/server/ChunkedEntitiesDocs.scala) { #implementation }

### Error handling

When the server processes requests, three kinds of errors can happen: the incoming request doesn’t match
any endpoint, the request does match an endpoint but is invalid (e.g. one parameter has a wrong type), or
an exception is thrown.

#### The incoming request doesn’t match any endpoint

In that case, the router constructed by endpoints4s can’t do anything. You have to deal with such
errors in the usual Play way: by using a custom `play.api.http.HttpErrorHandler`.

#### The incoming request is invalid

In that case, endpoints4s returns a “Bad Request” (400) response reporting all the errors in a
JSON array. You can change this behavior by overriding the
@scaladoc[handleClientErrors](endpoints4s.play.server.Urls) method.

#### An exception is thrown

If an exception is thrown during request decoding, or when running the business logic, or when
encoding the response, endpoints4s returns an “Internal Server Error” (500) response reporting
the error in a JSON array. You can change this behavior by overriding the
@scaladoc[handleServerError](endpoints4s.play.server.Endpoints) method.
