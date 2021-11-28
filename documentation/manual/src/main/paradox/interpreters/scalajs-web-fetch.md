# Scala.js web client (Fetch)

Web client using `Fetch`.

@coordinates[fetch-client] { platform=js }

@scaladoc[API documentation](endpoints4s.fetch.index)

The `Endpoints` interpreter fixes the type `Endpoint[A, B]` to a function
from `A` to `Result[B]`, where `Result` is abstract and is intended
to be defined by more specialized interpreters.

An example of such an interpreter is `endpoints4s.fetch.client.future.Endpoints`,
which fixes the `Result[A]` type to `scala.concurrent.Future[A]`.

This means that, given the following endpoint definition:

@@snip [EndpointsDocs.scala](/algebras/algebra/src/test/scala/endpoints4s/algebra/EndpointsDocs.scala) { #endpoint-definition }

It can be invoked as follows:

@@snip [EndpointsDocs.scala](/fetch/client/src/test/scala/endpoints4s/fetch/future/EndpointsDocs.scala) { #invocation }
