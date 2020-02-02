# Play framework

Client and server backed by [Play framework](https://www.playframework.com/).

## Client

~~~ scala expandVars=true
"org.julienrf" %% "endpoints-play-client" % "{{version}}"
~~~

[API documentation](unchecked:/api/endpoints/play/client/index.html)

The `Endpoints` interpreter fixes the `Endpoint[A, B]` type to a function from
`A` to `Future[B]`:

~~~ scala src=../../../../../play/client/src/main/scala/endpoints/play/client/Endpoints.scala#concrete-carrier-type
~~~

This means that, given the following endpoint definition:

~~~ scala src=../../../../../algebras/algebra/src/test/scala/endpoints/algebra/EndpointsDocs.scala#endpoint-definition
~~~

It can be invoked as follows:

~~~ scala src=../../../../../play/client/src/test/scala/endpoints/play/client/EndpointsDocs.scala#invocation
~~~

## Server

~~~ scala expandVars=true
"org.julienrf" %% "endpoints-play-server" % "{{version}}"
~~~

[API documentation](unchecked:/api/endpoints/play/server/index.html)

### `Endpoints`

The `Endpoints` interpreter provides a `routesFromEndpoints` operation that turns
a sequence of endpoints with their implementation into a `play.api.routing.Router.Routes`
value that can be integrated to your Play application.

For instance, given the following endpoint definition:

~~~ scala src=../../../../../algebras/algebra/src/test/scala/endpoints/algebra/EndpointsDocs.scala#endpoint-definition
~~~

It can be implemented as follows:

~~~ scala src=../../../../../play/server/src/test/scala/endpoints/play/server/EndpointsDocs.scala#implementation
~~~

In practice, the routes are put in a class taking an `endpoints.play.server.PlayComponents`
parameter. An HTTP server can then be started as in the following example:

~~~ scala src=../../../../../documentation/examples/documented/src/main/scala/counter/Counter.scala#main-only
~~~

### `ChunkedEntities`

The `ChunkedEntities` interpreter fixes the type `Chunks[A]` to `akka.stream.scaladsl.Source[A, _]`.

For instance, given the following chunked endpoint definition:

~~~ scala src=../../../../../algebras/algebra/src/test/scala/endpoints/algebra/ChunkedEntitiesDocs.scala#streamed-endpoint
~~~

It can be implemented as follows:

~~~ scala src=../../../../../play/server/src/test/scala/endpoints/play/server/ChunkedEntitiesDocs.scala#implementation
~~~

### Error handling

When the server processes requests, three kinds of errors can happen: the incoming request doesn’t match
any endpoint, the request does match an endpoint but is invalid (e.g. one parameter has a wrong type), or
an exception is thrown.

#### The incoming request doesn’t match any endpoint

In that case, the router constructed by *endpoints* can’t do anything. You have to deal with such
errors in the usual Play way: by using a custom `play.api.http.HttpErrorHandler`.

#### The incoming request is invalid

In that case, *endpoints* returns a “Bad Request” (400) response reporting all the errors in a
JSON array. You can change this behavior by overriding the
[handleClientErrors](unchecked:/api/endpoints/play/server/Urls.html#handleClientErrors(invalid:endpoints.Invalid):play.api.mvc.Result)
method.

#### An exception is thrown

If an exception is thrown during request decoding, or when running the business logic, or when
encoding the response, *endpoints* returns an “Internal Server Error” (500) response reporting
the error in a JSON array. You can change this behavior by overriding the
[handleServerError](unchecked:/api/endpoints/play/server/Endpoints.html#handleServerError(throwable:Throwable):play.api.mvc.Result)
method.
