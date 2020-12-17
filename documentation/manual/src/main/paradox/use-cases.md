# Use Cases

This page shows typical use cases where endpoints4s can provide value.

## Microservices

Describe the HTTP APIs between the services, and let endpoints4s implement
the clients and servers for these APIs:

@@snip [CommandsEndpoints.scala](/documentation/examples/cqrs/commands-endpoints/src/main/scala/cqrs/commands/CommandsEndpoints.scala) { #microservice-endpoint-description }

Invoking a service from another is as simple as a method call:

@@snip [PublicServer.scala](/documentation/examples/cqrs/public-server/src/main/scala/cqrs/publicserver/PublicServer.scala) { #microservice-endpoint-invocation }

endpoints4s takes care of correctly constructing the HTTP request and
decoding the HTTP response according to the endpoint description.

Maintenance effort is reduced: you only maintain the description of
the HTTP API, not its client and server implementations.

## Web Applications

Thanks to Scala.js it is possible to write the client-side part of a
web application in Scala. Then, endpoints4s simplifies client-server
communication by turning method calls into remote invocations.

Example of endpoint definition:

@@snip [PublicEndpoints.scala](/documentation/examples/cqrs/public-endpoints/src/main/scala/cqrs/publicserver/PublicEndpoints.scala) { #webapps-endpoint }

Corresponding invocation from the client-side:

@@snip [Main.scala](/documentation/examples/cqrs/web-client/src/main/scala/cqrs/webclient/Main.scala) { #webapps-invocation }

## Documenting a Web Service

Thanks to the separation between the description of an HTTP API and
its implementation, endpoints4s can also generate an OpenAPI document
for a given HTTP API description.

For instance, given the following endpoints descriptions:

@@snip [Counter.scala](/documentation/examples/documented/src/main/scala/counter/Counter.scala) { #documented-endpoints }

endpoints4s can produce the
[following OpenApi document](https://documented-counter.herokuapp.com/documentation.json).
