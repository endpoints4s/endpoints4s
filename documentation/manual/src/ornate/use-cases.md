# Use Cases

This page lists a few use cases where *endpoints* helps.

Combinations of these use cases multiply the value brought by *endpoints*!

## Microservices

Describe the HTTP APIs between the services, and let *endpoints* implements
the clients and servers for these APIs:

~~~ scala src=../../../examples/cqrs/commands-endpoints/src/main/scala/cqrs/commands/CommandsEndpoints.scala#microservice-endpoint-description
~~~

Invoking a service from another is as simple as a method call:

~~~ scala src=../../../examples/cqrs/public-server/src/main/scala/cqrs/publicserver/PublicServer.scala#microservice-endpoint-invocation
~~~

*endpoints* takes care of correctly constructing the request and
decoding the response according to the HTTP endpoint description.

Maintenance effort is reduced: you only maintain the description of
the HTTP API, not it’s client and server implementations.

## Web Applications

Thanks to Scala.js it is possible to write the client-side part of a
web application in Scala. Then, *endpoints* simplifies client-server
communication by turning method calls into remote invocations.

Endpoint definition:

~~~ scala src=../../../examples/cqrs/public-endpoints/src/main/scala/cqrs/publicserver/PublicEndpoints.scala#webapps-endpoint
~~~

Endpoint invocation:

~~~ scala src=../../../examples/cqrs/web-client/src/main/scala/cqrs/webclient/Main.scala#webapps-invocation
~~~

## Documenting a Web Service

Thanks to the separation between the description of an HTTP API and
its implementation, *endpoints* can also generate an OpenAPI document
for a given HTTP API description.

For instance, given the following endpoint description:

~~~ scala src=../../../examples/documented/src/main/scala/counter/Counter.scala#documented-endpoints
~~~

*endpoints* can produce the
[following OpenApi document](https://documented-counter.herokuapp.com/documentation.json).
