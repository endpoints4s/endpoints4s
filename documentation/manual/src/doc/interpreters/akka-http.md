# Akka HTTP

Client and server backed by [Akka HTTP](https://doc.akka.io/docs/akka-http/current/).

## Client

~~~ scala expandVars=true
"org.julienrf" %% "endpoints-akka-http-client" % "{{version}}"
~~~

[API documentation](unchecked:/api/endpoints/akkahttp/client/index.html)

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

### `ChunkedEntities`

The `ChunkedEntities` interpreter fixes the `Chunks[A]` type to `akka.stream.scaladsl.Source[A, _]`:

~~~ scala src=../../../../../akka-http/client/src/main/scala/endpoints/akkahttp/client/ChunkedEntities.scala#stream-type
~~~

This means that, given the following endpoint definition:

~~~ scala src=../../../../../algebras/algebra/src/test/scala/endpoints/algebra/ChunkedEntitiesDocs.scala#streamed-endpoint
~~~

It can be invoked as follows:

~~~ scala src=../../../../../akka-http/client/src/test/scala/endpoints/akkahttp/client/ChunkedEntitiesDocs.scala#invocation
~~~

## Server

~~~ scala expandVars=true
"org.julienrf" %% "endpoints-akka-http-server" % "{{version}}"
~~~

[API documentation](unchecked:/api/endpoints/akkahttp/server/index.html)

### `Endpoints`

The `Endpoints` interpreter fixes the `Endpoint[A, B]` type to something that,
given an implementation function `A => B`, returns an `akka.http.scaladsl.server.Route`
that can be integrated to your Akka HTTP application.

For instance, given the following endpoint definition:

~~~ scala src=../../../../../algebras/algebra/src/test/scala/endpoints/algebra/EndpointsDocs.scala#endpoint-definition
~~~

It can be implemented as follows:

~~~ scala src=../../../../../akka-http/server/src/test/scala/endpoints/akkahttp/server/EndpointsDocs.scala#implementation
~~~

Alternatively, there is also a method `implementedByAsync` that takes an implementing function
returning a `Future[B]`.

### `ChunkedEntities`

The `ChunkedEntities` interpreter fixes the `Chunks[A]` type to `akka.stream.scaladsl.Source[A, _]`.

For instance, given the following chunked endpoint definition:

~~~ scala src=../../../../../algebras/algebra/src/test/scala/endpoints/algebra/ChunkedEntitiesDocs.scala#streamed-endpoint
~~~

It can be implemented as follows:

~~~ scala src=../../../../../akka-http/server/src/test/scala/endpoints/akkahttp/server/ChunkedEntitiesDocs.scala#implementation
~~~

### Error handling

When the server processes requests, three kinds of errors can happen: the incoming request doesn’t match
any endpoint, the request does match an endpoint but is invalid (e.g. one parameter has a wrong type), or
an exception is thrown.

#### The incoming request doesn’t match any endpoint

In that case, the routes constructed by *endpoints* can’t do anything. You have to deal with such
errors in the usual Akka HTTP way: by using an implicit `akka.http.scaladsl.server.RejectionHandler`
having a `handleNotFound` clause.

#### The incoming request is invalid

In that case, *endpoints* returns a “Bad Request” (400) response reporting all the errors in a
JSON array. You can change this behavior by overriding the
[handleClientErrors](unchecked:/api/endpoints/akkahttp/server/Urls.html#handleClientErrors(invalid:endpoints.Invalid):akka.http.scaladsl.server.StandardRoute)
method.

#### An exception is thrown

If an exception is thrown during request decoding, or when running the business logic, or when
encoding the response, *endpoints* returns an “Internal Server Error” (500) response reporting
the error in a JSON array. You can change this behavior by overriding the
[handleServerError](unchecked:/api/endpoints/akkahttp/server/Endpoints.html#handleServerError(throwable:Throwable):akka.http.scaladsl.server.StandardRoute)
method.
