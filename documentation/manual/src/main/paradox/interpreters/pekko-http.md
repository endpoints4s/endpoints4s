# Pekko HTTP

Client and server backed by [Pekko HTTP](https://pekko.apache.org/docs/pekko/current/).

@@@ warning

As explained in the section
[Mixed versioning is not allowed](https://pekko.apache.org/docs/pekko/current/common/binary-compatibility-rules.html#mixed-versioning-is-not-allowed)
in the Pekko documentation, you have to make sure that all the
Pekko modules of your application have the same version.

For this reason, the interpreters `pekko-http-server` and
`pekko-http-client` have marked their dependency to Pekko as “provided”.

As a consequence, to use these interpreters you will have to
explicitly add a dependency on `pekko-stream`:

~~~ scala
"org.apache.pekko" %% "pekko-stream" % "<pekko-version>"
~~~

Where `<pekko-version>` is binary compatible and higher or equal to @var[pekko-version].

@@@

## Client

@coordinates[pekko-http-client]

@scaladoc[API documentation](endpoints4s.pekkohttp.client.index)

### `endpoints4s.pekkohttp.client.Endpoints`

The `Endpoints` interpreter fixes the `Endpoint[A, B]` type to a function
from `A` to `Future[B]`:

@@snip [Endpoints.scala](/pekko-http/client/src/main/scala/endpoints4s/pekkohttp/client/Endpoints.scala) { #endpoint-type }

This means that, given the following endpoint definition:

@@snip [EndpointsDocs.scala](/pekko-http/client/src/test/scala/endpoints4s/pekkohttp/client/EndpointsDocs.scala) { #endpoint-definition }

It can be invoked as follows:

@@snip [EndpointsDocs.scala](/pekko-http/client/src/test/scala/endpoints4s/pekkohttp/client/EndpointsDocs.scala) { #invocation }

### `endpoints4s.pekkohttp.client.ChunkedEntities`

The `ChunkedEntities` interpreter fixes the `Chunks[A]` type to `pekko.stream.scaladsl.Source[A, _]`:

@@snip [ChunkedEntities.scala](/pekko-http/client/src/main/scala/endpoints4s/pekkohttp/client/ChunkedEntities.scala) { #stream-type }

This means that, given the following endpoint definition:

@@snip [ChunkedEntitiesDocs.scala](/algebras/algebra/src/test/scala/endpoints4s/algebra/ChunkedEntitiesDocs.scala) { #streamed-endpoint }

It can be invoked as follows:

@@snip [ChunkedEntitiesDocs.scala](/pekko-http/client/src/test/scala/endpoints4s/pekkohttp/client/ChunkedEntitiesDocs.scala) { #invocation }

## Server

@coordinates[pekko-http-server]

@scaladoc[API documentation](endpoints4s.pekkohttp.server.index)

### `endpoints4s.pekkohttp.server.Endpoints`

The `Endpoints` interpreter fixes the `Endpoint[A, B]` type to something that,
given an implementation function `A => B`, returns an `pekko.http.scaladsl.server.Route`
that can be integrated to your Pekko HTTP application.

For instance, given the following endpoint definition:

@@snip [EndpointsDocs.scala](/pekko-http/server/src/test/scala/endpoints4s/pekkohttp/server/EndpointsDocs.scala) { #endpoint-definition }

It can be implemented as follows:

@@snip [EndpointsDocs.scala](/pekko-http/server/src/test/scala/endpoints4s/pekkohttp/server/EndpointsDocs.scala) { #implementation }

Alternatively, there is also a method `implementedByAsync` that takes an implementing function
returning a `Future[B]`.

### `endpoints4s.pekkohttp.server.ChunkedEntities`

The `ChunkedEntities` interpreter fixes the `Chunks[A]` type to `pekko.stream.scaladsl.Source[A, _]`.

For instance, given the following chunked endpoint definition:

@@snip [ChunkedEntitiesDocs.scala](/algebras/algebra/src/test/scala/endpoints4s/algebra/ChunkedEntitiesDocs.scala) { #streamed-endpoint }

It can be implemented as follows:

@@snip [ChunkedEntitiesDocs.scala](/pekko-http/server/src/test/scala/endpoints4s/pekkohttp/server/ChunkedEntitiesDocs.scala) { #implementation }

### Error handling

When the server processes requests, three kinds of errors can happen: the incoming request doesn’t match
any endpoint, the request does match an endpoint but is invalid (e.g. one parameter has a wrong type), or
an exception is thrown.

#### The incoming request doesn’t match any endpoint

In that case, the routes constructed by endpoints4s can’t do anything. You have to deal with such
errors in the usual Pekko HTTP way: by using an implicit `pekko.http.scaladsl.server.RejectionHandler`
having a `handleNotFound` clause.

#### The incoming request is invalid

In that case, endpoints4s returns a “Bad Request” (400) response reporting all the errors in a
JSON array. You can change this behavior by overriding the
@scaladoc[handleClientErrors](endpoints4s.pekkohttp.server.Urls) method.

#### An exception is thrown

If an exception is thrown during request decoding, or when running the business logic, or when
encoding the response, endpoints4s returns an “Internal Server Error” (500) response reporting
the error in a JSON array. You can change this behavior by overriding the
@scaladoc[handleServerError](endpoints4s.pekkohttp.server.Endpoints) method.
