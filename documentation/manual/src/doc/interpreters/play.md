# Play framework

Client and server backed by [Play framework](https://www.playframework.com/).

## Client

~~~ scala expandVars=true
"org.julienrf" %% "endpoints-play-client" % "{{version}}"
~~~

[API documentation](api:endpoints.play.client.package)

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

[API documentation](api:endpoints.play.server.package)

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

~~~ scala src=../../../../../documentation/examples/quickstart/server/src/main/scala/quickstart/Main.scala#main-only
~~~
