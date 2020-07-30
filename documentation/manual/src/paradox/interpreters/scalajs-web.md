# Scala.js web client

Web client using `XMLHttpRequest`.

@coordinates[xhr-client] { platform=js }

@scaladoc[API documentation](endpoints4s.xhr.index)

The `Endpoints` interpreter fixes the type `Endpoint[A, B]` to a function
from `A` to `Result[B]`, where `Result` is abstract and is intended
to be defined by more specialized interpreters.

An example of such an interpreter is `endpoints4s.xhr.client.future.Endpoints`,
which fixes the `Result[A]` type to `scala.concurrent.Future[A]`.

This means that, given the following endpoint definition:

@@snip [EndpointsDocs.scala](/algebras/algebra/src/test/scala/endpoints4s/algebra/EndpointsDocs.scala) { #endpoint-definition }

It can be invoked as follows:

@@snip [EndpointsDocs.scala](/xhr/client/src/test/scala/endpoints4s/xhr/future/EndpointsDocs.scala) { #invocation }

Another example is `endpoints4s.xhr.client.faithful.Endpoints`, which fixes
the `Result[A]` type to `faithful.Future[A]`. This interpreter requires
an additional dependency (which, in turn, depends on the
[faithful](https://github.com/julienrf/faithful) library):

@coordinates[xhr-client-faithful] { platform=js }
