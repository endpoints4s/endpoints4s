# sttp

Client backed by [sttp](https://github.com/softwaremill/sttp).

@@@vars
~~~ scala
"org.endpoints4s" %% "sttp-client" % "$version$"
~~~
@@@

@scaladoc[API documentation](endpoints.sttp.client.index)

The `Endpoints` interpreter is parameterized by an sttp backend
of type `SttpBackend[R, Nothing]`, for some type constructor `R[_]`.

The `Endpoint[A, B]` type is fixed as follows:

@@snip [Endpoints.scala](/sttp/client/src/main/scala/endpoints/sttp/client/Endpoints.scala) { #endpoint-type }

This means that, given the following endpoint definition:

@@snip [EndpointsDocs.scala](/algebras/algebra/src/test/scala/endpoints/algebra/EndpointsDocs.scala) { #endpoint-definition }

It can be invoked as follows with the `HttpURLConnectionBackend`,
for instance:

@@snip [EndpointsDocs.scala](/sttp/client/src/test/scala/endpoints/sttp/client/EndpointsDocs.scala) { #invocation }
