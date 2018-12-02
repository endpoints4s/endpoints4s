# Akka HTTP

Client and server backed by [Akka HTTP](https://doc.akka.io/docs/akka-http/current/).

## Client

~~~ scala expandVars=true
"org.julienrf" %% "endpoints-akka-http-client" % "{{version}}"
~~~

[API documentation](api:endpoints.akkahttp.client.package)

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

## Server

~~~ scala expandVars=true
"org.julienrf" %% "endpoints-akka-http-server" % "{{version}}"
~~~

[API documentation](api:endpoints.akkahttp.server.package)

The `Endpoints` interpreter fixes the `Endpoint[A, B]` type to something that,
given an implementation function `A => B`, returns an `akka.http.scaladsl.server.Route`
that can be integrated to your Akka HTTP application.

For instance, given the following endpoint definition:

~~~ scala src=../../../../../algebras/algebra/src/test/scala/endpoints/algebra/EndpointsDocs.scala#endpoint-definition
~~~

It can be implemented as follows:

~~~ scala src=../../../../../akka-http/server/src/test/scala/endpoints/akkahttp/server/EndpointsDocs.scala#implementation
~~~
