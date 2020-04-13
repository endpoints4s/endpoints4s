endpoints
=========

@@@ index
* [Use Cases](use-cases.md)
* [Quick Start](quick-start.md)
* [Design in a Nutshell](design.md)
* [Algebras and Interpreters](algebras-and-interpreters.md)
* [Guides](guides.md)
* [Comparison with Similar Tools](comparison.md)
* [Talks and Publications](talks.md)
* [Release and Compatibility Notes](release-and-compatibility-notes.md)
@@@

*endpoints* is a Scala library for remote communication. It ensures
that HTTP servers, HTTP clients, and documentation always agree on the same protocol.

- **maintenance is simplified**: the API documentation is automatically updated when an endpoint is modified ;
- errors are raised at **compile-time** if endpoints are invoked with incompatible parameters.

Servers, clients and documentation are *all* derived from a *single* source of truth describing the underlying
protocol details (e.g., which verb, path, query parameters, headers, authentication strategy, etc. to
use). For instance, here is an endpoint for incrementing a counter. It uses the HTTP verb `POST`, the URL path
`/increment`, a JSON request entity containing an `Increment` value, and it returns an empty response.

@@snip [CounterEndpoints.scala](/documentation/examples/quickstart/endpoints/src/main/scala/quickstart/CounterEndpoints.scala) { #endpoint-definition }

From the client perspective, calling an HTTP endpoint is as simple as calling a function:

@@snip [CounterClientFuture.scala](/documentation/examples/quickstart/client/src/main/scala/quickstart/CounterClientFuture.scala) { #endpoint-invocation }

Conversely, from the server perspective implementing an HTTP endpoint is as simple as implementing a function:

@@snip [CounterServer.scala](/documentation/examples/quickstart/server/src/main/scala/quickstart/CounterServer.scala) { #endpoint-implementation }

The *endpoints* library takes care of constructing the HTTP requests and responses and decoding the server
responses or client requests into high-level data types according to the endpoint descriptions.

In contrast with @ref[most other approaches](comparison.md), *endpoints* is a pure, "*vanilla*", Scala library.
**No code generation**. **No macros**. **IDE friendly**. Endpoint descriptions are **first-class Scala values**,
which can be reused, combined, and abstracted over.

The library currently supports the following backends:

- clients: Akka-Http, Play-WS, sttp, scalaj, and XMLHttpRequest (Scala.js) ;
- servers: Akka-Http, Play, and http4s ;
- documentation: OpenAPI ;
- JSON is supported via Circe, Play-Json, or ujson ;

… but the library is designed to be **extensible**, anyone can:

- implement a new interpreter for the existing endpoint descriptions (e.g. generation of RAML
  documentation, finch client and server backend, etc.) ;
- add new descriptions to the existing ones (e.g. to define an application-specific authentication
  strategy).

## Getting started

- Have a look at the @ref[quick start](quick-start.md) guide to understand
  in a few minutes what the library does and how to setup a project ;
- Check out the @ref[use cases](use-cases.md) to know the typical problems that *endpoints* addresses ;
- Browse the @scaladoc[API documentation](endpoints.index) or the
  [samples](https://github.com/julienrf/endpoints/tree/master/documentation/examples) ;
- Meet the community in the [gitter room](https://gitter.im/julienrf/endpoints).

## Contributing

See the [Github repository](https://github.com/julienrf/endpoints).

## Sponsors

[Bestmile](https://bestmile.com) supports engineering work on the project.
