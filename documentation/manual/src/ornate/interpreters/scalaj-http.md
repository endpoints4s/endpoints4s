# scalaj-http

Client interpreter backed by [scalaj-http](https://github.com/scalaj/scalaj-http).

~~~ scala expandVars=true
"org.julienrf" %% "endpoints-scalaj-client" % "{{version}}"
~~~

[API documentation](api:endpoints.scalaj.client.package)

The `Endpoints` interpreter fixes the `Endpoint[A, B]` type
to a type that provide methods to invoke the endpoint synchronously
or asynchronously.

Given the following endpoint definition:

~~~ scala src=../../../../../algebras/algebra/src/test/scala/endpoints/algebra/EndpointsDocs.scala#endpoint-definition
~~~

It can be asynchronously invoked as follows:

~~~ scala src=../../../../../scalaj/client/src/test/scala/endpoints/scalaj/client/EndpointsDocs.scala#invocation
~~~
