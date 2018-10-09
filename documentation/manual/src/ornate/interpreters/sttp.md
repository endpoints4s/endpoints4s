# sttp

Client backed by [sttp](https://github.com/softwaremill/sttp).

~~~ scala expandVars=true
"org.julienrf" %% "endpoints-sttp-client" % "{{version}}"
~~~

[API documentation](api:endpoints.sttp.client.package)

The `Endpoints` interpreter is parameterized by an sttp backend
of type `SttpBackend[R, Nothing]`, for some type constructor `R[_]`.

The `Endpoint[A, B]` type is fixed as follows:

~~~ scala src=../../../../../sttp/client/src/main/scala/endpoints/sttp/client/Endpoints.scala#endpoint-type
~~~

This means that, given the following endpoint definition:

~~~ scala src=../../../../../algebras/algebra/src/test/scala/endpoints/algebra/EndpointsDocs.scala#endpoint-definition
~~~

It can be invoked as follows with the `HttpURLConnectionBackend`,
for instance:

~~~ scala src=../../../../../sttp/client/src/test/scala/endpoints/sttp/client/EndpointsDocs.scala#invocation
~~~
