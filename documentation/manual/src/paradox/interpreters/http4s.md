# http4s

Client and server backed by [http4s](http://http4s.org).

## Client

@@@vars
~~~ scala
"org.julienrf" %% "endpoints-http4s-client" % "$version$"
~~~
@@@

@scaladoc[API documentation](endpoints.http4s.client.index)

### `Endpoints`

The `Endpoints` interpreter fixes the `Endpoint[A, B]` type to a `Kleisli[Effect, A, B]` aka a function
from `A` to `Effect[B]`, where `Effect[_]` can be any type constructor `F[_]` with a valid `cats.effect.Sync` instance (e.g. `cats.effect.IO` or `monix.eval.Task`) :

@@snip [Endpoints.scala](/http4s/client/src/main/scala/endpoints/http4s/client/Endpoints.scala) { #endpoint-type }

This means that, given the following endpoint definition:

@@snip [EndpointsDocs.scala](/algebras/algebra/src/test/scala/endpoints/algebra/EndpointsDocs.scala) { #endpoint-definition }

It can be invoked as follows using `IO`:

@@snip [EndpointsDocs.scala](/http4s/client/src/test/scala/endpoints/http4s/client/EndpointsDocs.scala) { #invocation }

## Server

@@@vars
~~~ scala
"org.julienrf" %% "endpoints-http4s-server" % "$version$"
~~~
@@@

@scaladoc[API documentation](endpoints.http4s.server.index)

### `Endpoints`

The `Endpoints` interpreter provides a `routesFromEndpoints` operation that
turns a sequence of endpoints with their implementation into an `org.http4s.HttpRoutes[F]`
value that can be integrated to your http4s application.

For instance, given the following endpoint definition:

@@snip [EndpointsDocs.scala](/algebras/algebra/src/test/scala/endpoints/algebra/EndpointsDocs.scala) { #endpoint-definition }

It can be implemented as follows:

@@snip [EndpointsDocs.scala](/http4s/server/src/test/scala/endpoints/http4s/server/EndpointsDocs.scala) { #implementation }

The result is a regular value of type `org.http4s.HttpRoute[IO]` that can be integrated in your application like
any other http4s service.

### Error handling

When the server processes requests, three kinds of errors can happen: the incoming request doesn’t match
any endpoint, the request does match an endpoint but is invalid (e.g. one parameter has a wrong type), or
an exception is thrown.

#### The incoming request doesn’t match any endpoint

In that case, the router constructed by *endpoints* can’t do anything. You have to deal with such
errors in the usual http4s way (usually, by adding a `.orNotFound` call to your application
services).

#### The incoming request is invalid

In that case, *endpoints* returns a “Bad Request” (400) response reporting all the errors in a
JSON array. You can change this behavior by overriding the
@scaladoc[handleClientErrors](endpoints.http4s.server.EndpointsWithCustomErrors) method.

#### An exception is thrown

If an exception is thrown during request decoding, or when running the business logic, or when
encoding the response, *endpoints* returns an “Internal Server Error” (500) response reporting
the error in a JSON array. You can change this behavior by overriding the
@scaladoc[handleServerError](endpoints.http4s.server.EndpointsWithCustomErrors) method.
