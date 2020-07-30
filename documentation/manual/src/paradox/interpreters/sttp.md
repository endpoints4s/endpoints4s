# sttp

Client backed by [sttp](https://github.com/softwaremill/sttp).

@coordinates[sttp-client]

@scaladoc[API documentation](endpoints4s.sttp.client.index)

The `Endpoints` interpreter is parameterized by an sttp backend
of type `SttpBackend[R, Nothing]`, for some type constructor `R[_]`.

The `Endpoint[A, B]` type is fixed as follows:

@@snip [Endpoints.scala](/sttp/client/src/main/scala/endpoints4s/sttp/client/Endpoints.scala) { #endpoint-type }

This means that, given the following endpoint definition:

@@snip [EndpointsDocs.scala](/algebras/algebra/src/test/scala/endpoints4s/algebra/EndpointsDocs.scala) { #endpoint-definition }

It can be invoked as follows with the `HttpURLConnectionBackend`,
for instance:

@@snip [EndpointsDocs.scala](/sttp/client/src/test/scala/endpoints4s/sttp/client/EndpointsDocs.scala) { #invocation }
