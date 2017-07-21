# Tutorial

This tutorial introduces the main features of the endpoints library
based on a complete application.

## Introduction

This section describes the application domain and architecture.
For the sake of brevity, this tutorial only shows the parts of the code
that rely on the endpoints library. The complete
source code (less than 1k lines of code) is available
[here](https://github.com/julienrf/endpoints/tree/master/examples/cqrs).

The goal of the application is to help monitoring home resource consumption (like,
for instance, electricity or water).
It provides a dashboard listing all the meters the user
is interested in and display charts showing their evolution over time:

![](meters.png)

Users are able to create meters, to add records (the value of a meter
at a given time) to them, and to visualize their evolution over time.

In order to exercise the features of the endpoints library, the application is
broken down into several sub-applications that communicate
over HTTP.

First, our user interface is web-based, which means that we have a
`web-client` and a `public-server`:

~~~ mermaid
graph LR
  web-client --> public-server
~~~

In this diagram, the `web-client` application is a Scala.js application running in
a web browser and communicating with the `public-server` through HTTP.

Second, the `public-server` itself is broken down into smaller pieces, following a
[CQRS](https://martinfowler.com/bliki/CQRS.html) pattern:

~~~ mermaid
graph LR
  web-client --> public-server
  subgraph public-server
    public-server --> commands
    public-server --> queries
    queries --> commands
  end
~~~

The `public-server` itself delegates commands processing
to the `commands` microservice and queries processing to the `queries`
microservice.

In case you are not familiar with CQRS, the idea is to have distinct microservices
handle *queries* (actions that *read* the state of the application) and *commands*
(actions that *update* the state of the application). As a benefit, the system is
more resilient (if the commands service is down, the system still works in
read-only) and more scalable (we could add multiple `queries` nodes).

The `commands` microservice validates commands and
updates the state of the system. State changes are represented as
an infinitely growing sequence of events. Then, the `queries` microservice
builds a model from these events (hence the arrow from `queries` to
`commands`). This pattern is known as
[event sourcing](https://martinfowler.com/eaaDev/EventSourcing.html).

This tutorial shows how these services communicate with each
other by using the endpoints library.

Let’s start with the `commands` microservice!

## Describing an HTTP API

### Project layout

In order to get our `queries` service successfully communicate with the
`commands` service, they have to agree on a communication protocol.

~~~ mermaid
graph BT
  queries -.-> commands-endpoints
  commands -.-> commands-endpoints
  queries --> commands
~~~

We introduce a new artifact named `commands-endpoints` that provides the
*descriptions* of the endpoints of the `commands` service. The `queries` service
uses these descriptions to derive a *client* invoking the endpoints, while the
`commands` service uses these descriptions to derive a *server* handling
incoming requests. (Note that in our diagrams, dotted links mean classpath
dependencies, while solid links mean communication over the wire)

Our `commands-endpoints` project has to depend on the *endpoints algebra API*, which
provides means of describing HTTP endpoints:

~~~ mermaid
graph BT
  queries -.-> commands-endpoints
  commands -.-> commands-endpoints
  queries --> commands
  commands-endpoints -.-> algebra["endpoints-algebra-circe"]
  style algebra fill:#eee;
~~~

### Describing HTTP endpoints

Our `commands` service to handles two actions:
- apply a command ;
- read the event log.

Here is how we define a communication protocol for these actions:

~~~ scala src=../../../examples/cqrs/commands-endpoints/src/main/scala/cqrs/commands/CommandsEndpoints.scala#endpoints
~~~

Let’s explain this code line by line.

First, we import the `Endpoints` and `CirceEntities` traits from the `endpoints.algebra` package.
All the traits that live in the `algebra` package provide methods for *describing* HTTP endpoints.
In our case, the [Endpoints](api:endpoints.algebra.Endpoints) trait provides the core methods
while the (optional) [CirceEntities](api:endpoints.algebra.CirceEntities) trait allows us to
use [circe](https://circe.github.io/circe/) encoders and decoders to marshal JSON entities in
requests and responses.

Second, we define our communication protocol within a trait, `CommandsEndpoints`, that extends
the algebra interfaces.

Then, we describe the HTTP endpoint for applying a command. Because commands update the state
of the system and because state updates are represented by events, our HTTP endpoint handles
requests carrying a `Command` and returns responses carrying an `Option[StoredEvent]` (in
case of failure, the response is `None`). Thus, the type of our `command` member is
`Endpoint[Command, Option[StoredEvent]]`. The `Endpoint` type is provided by the endpoints
library, while the types `Command` and `StoredEvent` are defined by our application. Their
definition is omitted for the sake of brevity, but they are plain case classes.

We describe the `command` endpoint by using the `endpoint` constructor, which takes two
parameters, one describing the request and the other describing the response.

Let’s detail the request description:
- the `post` method describes a request that uses the HTTP verb `POST`,
- the `path / "command"` expression describes the URL `/command`,
- the `jsonRequest[Command]` expression describes a JSON entity containing a `Command`.

The response is simply defined by the expression `jsonResponse[Option[StoredEvent]]`,
which describes an HTTP response whose entity is a JSON document containing an
`Option[StoredEvent]`.

As you can see, we describe our HTTP endpoints by combining plain Scala expressions.
The algebra interfaces provide methods that define the way we can combine these
expressions to build a complete HTTP endpoint description.

Now, let’s have a look at the `events` endpoint description. This endpoint returns
the events that happened after a given optional timestamp (or all the events, if
no timestamp is supplied). Thus, its type is `Endpoint[Option[Long], Seq[StoredEvent]]`.

The request description uses the method `get`, meaning that the request uses the
`GET` HTTP verb. The URL is described by the expression
`path / "events" /? optQs[Long]("since")`. Let’s break down this expression into
smaller parts:
- the beginning, `path / "events"` is similar to the URL description
  of the `command` endpoint: it describes an URL whose path is `/events` ;
- the `optQs[Long]("since")` expression describes an optional query string parameter
  of type `Long` and named `since` ;
- finally, the `/?` operator combines the path description and the query string
  description: our URL is described by the path `/events` followed by a query
  string containing an optional parameter `since`.

Last, the response is defined by the `jsonResponse[Seq[StoredEvent]]`
expression, which describes an HTTP response whose entity is a JSON document
containing a `Seq[StoredEvent]`.

### Summary

In this section we have seen that, in order to get two applications communicate
with each other, we first have to define a description of the HTTP endpoints
they will use. This one is defined in a sub-project which is depended on by
both the client and the server implementations.

The description of HTTP endpoints is achieved with plain Scala expressions
describing all the characteristics (verb, URL, entities, etc.) of each endpoint.

## Deriving a server from a service description

In this section we see how to implement a service from the service description
defined in the previous section.

The endpoints library provides *interpreters* that breathe life to inert service
descriptions.

A “server” interpreter implements an HTTP server that processes incoming requests
and calls the actual implementation of your service endpoints.

In this tutorial we will use an interpreter that uses
[Play framework](http://playframework.com) as the underlying HTTP server. Thus, we
add the following dependency to the `commands` project:

~~~ mermaid
graph BT
  queries -.-> commands-endpoints
  commands -.-> commands-endpoints
  queries --> commands
  commands-endpoints -.-> algebra["endpoints-algebra-circe"]
  commands -.-> interpreter["endpoints-play-server-circe"]
  style algebra fill:#eee;
  style interpreter fill:#eee;
~~~

Then we can create a `Commands` object interpreting the service description as a
server as follows:

~~~ scala src=../../../examples/cqrs/commands/src/main/scala/cqrs/commands/Commands.scala#server
~~~

Again, let’s detail line by line the above code.

First, we import the [`Endpoints`](api:endpoints.play.server.Endpoints) and
[`CirceEntities`](api:endpoints.play.server.CirceEntities) *interpreters* from the
`endpoints.play.server` package. Interpreters always have the same name as the
algebra interface they implement, but they are not located in the `endpoints.algebra`
package. The `endpoints.play.server` package contains interpreters that rely
on Play framework to implement an HTTP server.

Then, we extend the `CommandsEndpoints` trait that we defined in the previous section and we
mix the `Endpoints` and `CirceEntities` interpreters into it. That’s really the fact that
we mix the interpreters into our service description that is responsible of giving it a
concrete semantics.

The semantics provided by this interpreter consists in processing incoming requests
to extract the information we are interested in (for instance, a query string parameter,
or the request entity), and building HTTP responses from the values returned by the service
implementation. In the middle of this process, we have to supply the actual service implementation.

We achieve this by calling the `implementedBy` method on our endpoints. In our case, we just
delegate to a `CommandsService` object. The underlying implementation details are not relevant
for this tutorial, but let’s just have a look at the type signature of these methods `apply`
and `events`:

~~~ scala src=../../../examples/cqrs/commands/src/main/scala/cqrs/commands/CommandsService.scala#signatures
~~~

As you can see, the `play.server` interpreter takes care of decoding HTTP requests and
encoding HTTP responses according to our endpoint descriptions, so that we can focus
on implementing the actual logic of our service in terms of high-level data types.

Finally, we create a Play `Routes` value by using the `routesFromEndpoint` method (provided by
the interpreter), which takes implemented endpoints as parameters and returns a Play router.

At this point, we can effectively start our microservice by using the usual Play API. A minimal
program achieving this would look like the following:

~~~ scala
import play.core.server.NettyServer
NettyServer.fromRouter()(Commands.routes)
~~~

That’s it.

### Summary

In this section we have mixed an interpreter to a service description
to give it a “server” semantics, which effectively decodes HTTP requests and
encodes HTTP responses according to the service description.

## Deriving a client from a service description

This section explains how to apply a “client” interpreter to a service description
and how to use it to effectively communicate with the server.

A “client” interpreter is the dual of a server interpreter: it encodes
HTTP requests and decodes HTTP responses.

Our tutorial keeps using the Play framework stack and use in particular its
`WSClient` as the underlying HTTP client. Thus, we add the following
 dependency to the `queries` project:

~~~ mermaid
graph BT
  queries -.-> interpreter2["endpoints-play-client-circe"]
  queries -.-> commands-endpoints
  queries --> commands
  commands -.-> commands-endpoints
  commands-endpoints -.-> algebra["endpoints-algebra-circe"]
  commands -.-> interpreter["endpoints-play-server-circe"]
  style algebra fill:#eee;
  style interpreter fill:#eee;
  style interpreter2 fill:#eee;
~~~

Then we can create an `eventLog` object interpreting the service description as
a client as follows:

~~~ scala src=../../../examples/cqrs/queries/src/main/scala/cqrs/queries/QueriesService.scala#event-log-client
~~~

The pattern is similar to the code applying a server interpreter: we extend our `CommandsEndpoints`
trait and mix the [`endpoints.play.client.Endpoints`](api:endpoints.play.client.Endpoints)
and [`endpoints.play.client.CirceEntities`](api:endpoints.play.client.CirceEntities) interpreters to it.

The `commandsBaseUrl` and `wsClient` parameters (whose definition is not shown here for brevity)
define the base URL of the commands service
(e.g. `http://192.168.1.50`) and the Play `WSClient` instance to use to effectively perform
the HTTP requests.

Our queries service builds a model of the state of the application by periodically reading the events
that are appended to the log. To achieve this, it calls the `events` endpoint of the commands
service as follows:

~~~ scala src=../../../examples/cqrs/queries/src/main/scala/cqrs/queries/QueriesService.scala#invocation
~~~

From the perspective of a client, and endpoint is modeled as a function that takes as parameter
the information carried by the request and eventually returns the information carried by
the response.

In our case, the `eventLog.events` endpoint is the function we want to invoke. We can
write its type signature as follows:

~~~ scala
eventLog.events: Option[Long] => Future[Seq[StoredEvent]]
~~~

However, if you remember, we [previously](#describing-http-endpoints) defined `events` as a member
of type `Endpoint[Option[Long], Seq[StoredEvent]]`. But now we see that its type is different.
This works because `Endpoint[A, B]` is an abstract type member that is refined by
interpreters. Our client interpreter defines the `Endpoint` type as follows:

~~~ scala src=../../../../play/client/src/main/scala/endpoints/play/client/Endpoints.scala#concrete-carrier-type
~~~

Let’s read again the code that invokes the `events` endpoint:

~~~ scala src=../../../examples/cqrs/queries/src/main/scala/cqrs/queries/QueriesService.scala#invocation
~~~

We supply a parameter, `maybeLastEventTimestamp`, that contains the timestamp of the last event that
we applied to our projection (or `None` if we want to rebuild the entire projection).

The invocation returns a `Future[Seq[StoredEvent]]`, we `map` it transform the events into
an updated model of the system.

### Summary

We have seen how to derive a client from a service description and how to use
this client to communicate with the server (that was derived from the same service
description).

The communication between microservices is achieved through a statically typed API.

From a developer perspective, remotely invoking an endpoint of a microservice
consists in calling a function.

## Multiplexed endpoints

This section shows how the endpoints of the “queries” microservice are defined.
This microservice supports query operations like “find all”, “find by id”, etc.

Since this microservice is only called internally by our public server
we don’t really care about using a nice REST interface for our communication.

For instance, instead of using one HTTP endpoint per operation (`findAll`,
`findById`, etc.) we can define a single HTTP endpoint taking as parameter
either a “find all” type of query or a “find by id” type of query, and
returning the corresponding result(s).

Endpoints that handle several operations at once are called *multiplexed
endpoints*.

### Multiplexed endpoint description

The difference between a non-multiplexed endpoint and a multiplexed endpoint
is that the former has one type describing the HTTP request and one type
describing the HTTP response, whereas in a multiplexed endpoint several
types of request are supported, and the type of the response depends on the
actual type of the request.

We can see how this is reflected in the types. A non-multiplexed endpoint
for the “find by id” operation would describe a request carrying an id
of type `String` and a response carrying an `Option[Meter]`. The resulting
endpoint would have type
[`Endpoint[String, Option[Meter]]`](api:endpoints.algebra.Endpoints@Endpoint[A,B]).

If we wanted to also handle the “find all” operation with the same endpoint,
what would be the type of the response description? In the case of the “find
by id” operation, this would still be `Option[Meter]`, but in the case of the
“find all” operation, this would be `List[Meter]`. It is worth noting
that the type of the response depends on the type of the request.
Non-multiplexed endpoints can not express that.

Here is how we can define a multiplexed endpoint handling both operations:

~~~ scala src=../../../examples/cqrs/queries-endpoints/src/main/scala/cqrs/queries/QueriesEndpoints.scala#mux-endpoint
~~~

The type of this multiplexed endpoint is
[`MuxEndpoint[QueryReq, QueryResp, Json]`](api:endpoints.algebra.Endpoints%40MuxEndpoint%5BReq%3C%3Aendpoints.algebra.MuxRequest%2CResp%2CTransport%5D).
It describes an HTTP endpoint that handles request types that are subtypes of `QueryReq`
and response types that subtypes of `QueryResp`, and that uses JSON documents to marshal information.

The `QueryReq` type defines the possible types of requests:

~~~ scala src=../../../examples/cqrs/queries-endpoints/src/main/scala/cqrs/queries/QueriesEndpoints.scala#mux-requests
~~~

It is defined as a `sealed trait` that extends `MuxRequest` and whose each alternative (`FindAll`
and `FindById`) sets the `Response` type member to its corresponding response type.

The `FindById` request type carries the information of the “find by id” operation:
the `id` of the meter to look up, and an optional event timestamp
that can be used to get consistency between writes and reads.

The `FindAll` request type is the type of requests performing a “find all” operation.

> {.note}
> Alternatives *have to* be qualified as `final`, otherwise the type system will
> fail to compile calls to the endpoint.

The `QueryResp` type defines the possible types of responses as a regular
algebraic data type:

~~~ scala src=../../../examples/cqrs/queries-endpoints/src/main/scala/cqrs/queries/QueriesEndpoints.scala#mux-responses
~~~

The `MaybeResource` type carries the information of a response to a “find
by id” request (an optional `Meter`), and the `ResourceList` type carries
the information of a response to a “find all” request (a list of `Meter`s).

### Invoking a multiplexed endpoint

Our public server delegates to the queries service by invoking the
multiplexed endpoints defined in the previous section.

To invoke an endpoint as a client, we first have to derive a client for
the endpoint descriptions:

~~~ scala src=../../../examples/cqrs/public-server/src/main/scala/cqrs/publicserver/QueriesClient.scala
~~~

The process is the same as in the first part of the tutorial: we extend
the trait that defines the endpoint descriptions (`QueriesEndpoints`)
with traits that provide a client implementation (here, using the play framework
under the hood).

Once we have derived a client implementation, we can invoke the `query` endpoint
like so:

~~~ scala src=../../../examples/cqrs/public-server/src/main/scala/cqrs/publicserver/PublicServer.scala#invocation
~~~

We first instantiate the client and then invoke the `query` endpoint with a `FindAll` request.
We get a result of type `Future[ResourceList]`.

We can also invoke the “find by id” operation as follows:

~~~ scala src=../../../examples/cqrs/public-server/src/main/scala/cqrs/publicserver/PublicServer.scala#invocation-find-by-id
~~~

Note that here the return type is `Future[MaybeResource]`.

In order to get the `query` endpoint return different result types according to the type
of the value that is passed as parameter (`FindAll` vs `FindById`), we use an advanced
Scala feature named “path-dependent types”.

From a client point of view, a simplified type signature of the `apply` method of a `MuxEndpoint`
could be the following:

~~~ scala
trait MuxEndpoint[Req <: MuxRequest, Resp, Transport] {
  def apply(req: Req): Future[req.Response]
}
~~~

Translated in english, it means that one can invoke a `MuxEndpoint` by passing it a `Req` parameter,
and the result is a `Future[req.Response]`. The important part is the `req.Response` type: that’s
the `Response` type member of the `req` parameter. Thus, if we pass a `FindAll` request, we get
a result of type `ResourceList` because `FindAll` defines its `Response` type to `ResourceList`.

### Implementing a multiplexed endpoint

Our queries microservice uses Play framework as an underlying HTTP server. Thus, we derive
a server implementation of the `QueriesEndpoint` by mixing the `endpoints.play.server.Endpoints`
trait in it.

Then, the essence of the implementation consists in pattern matching on the supplied `QueryReq`
parameter:

~~~ scala src=../../../examples/cqrs/queries/src/main/scala/cqrs/queries/Queries.scala#multiplexed-impl-essence
~~~

Here we use a `service` abstraction that contains the actual implementation of the operations.
We just transform the results of these operations into the `Response` type corresponding
to each `QueryReq` (e.g. `MaybeResource` in the case of `FindById`, etc.).

The complete implementation of the `query` multiplexed endpoint is a bit more complex:

~~~ scala src=../../../examples/cqrs/queries/src/main/scala/cqrs/queries/Queries.scala#multiplexed-impl
~~~

We wrapped our match expression into a `MuxHandlerAsync`, whose definition is the following:

~~~ scala src=../../../../play/server/src/main/scala/endpoints/play/server/Endpoints.scala#mux-handler-async
~~~

This complex type signature is necessary to check that our implementation effectively
returns a value that corresponds to the `Response` type member of the passed request.

### Summary

A multiplexed endpoint is an HTTP endpoint that can handle several operations. It can be useful
to implement an internal communication protocol.

To describe a multiplexed endpoint you have to first reify the possible request types and their
respective response type as data types.

## Web client {#scalajs-client}

The previous sections have shown how to achieve internal communication between the microservices of our
`public-server` application.

This section describes the implementation of the HTTP API between external clients and our public
server.

~~~ mermaid
graph LR
  web-client --> public-server
~~~

The difference with the previous sections is that since this API is public we really want to reuse most of the
features of the HTTP protocol instead of re-implementing them on top of HTTP. For instance, requesting a non-
existing resource should return a 404 (Not Found) response (whereas in our internal `query` service we would get
a 200 (OK) response containing a `MaybeResource` entity whose `value` would be `None`).

For the sake of illustration, our `web-client` will use this public API.

### Public API description

As usual, we introduce a `public-endpoints` project containing the endpoints description:

~~~ mermaid
graph BT
  web-client -.-> interpreter2["endpoints-xhr-client-circe"]
  web-client -.-> public-endpoints
  web-client --> public-server
  public-server -.-> public-endpoints
  public-endpoints -.-> algebra["endpoints-algebra-circe"]
  public-server -.-> interpreter["endpoints-play-server-circe"]
  style algebra fill:#eee;
  style interpreter fill:#eee;
  style interpreter2 fill:#eee;
~~~

The `endpoints-xhr-client-circe` dependency provides a Scala.js interpreter that derives a client from endpoint
descriptions.

The public endpoints description defines four endpoints: `listMeters`, `getMeter`, `createMeter`
and `addRecord`:

~~~ scala src=../../../examples/cqrs/public-endpoints/src/main/scala/cqrs/publicserver/PublicEndpoints.scala#public-endpoints
~~~

We have already seen most of the used combinators that describe the endpoints. The `getMeter` endpoint
uses some new combinators, though:

~~~ scala src=../../../examples/cqrs/public-endpoints/src/main/scala/cqrs/publicserver/PublicEndpoints.scala#get-meter
~~~

The URL is described by the expression `metersPath / segment[UUID]`, which means the `/meters`
URL prefix followed by a segment containing an `UUID`. A `segment[X]` expression describes
a path segment that maps to a value of type `X`.

It is worth noting that the endpoint library provides no support for `UUID`s, but this one
can retroactively be added (as we do in this example): the `segment[A]` method takes an implicit
parameter of type `Segment[A]` that defines how to encode or decode the `A` value into a
path segment (thus, invoking `segment[X]` with an unsupported type `X` would raise an error at
compile-time). Since this encoding or decoding process is the responsibility of the interpreter,
and since we are only describing the endpoint, we added an abstract implicit method of type
`Segment[UUID]`, which will be implemented by interpreters.

The response of the `getMeter` endpoint is described by the expression
`option(jsonResponse[Meter])`, which means that it
can optionally be empty (for instance if a client queries this endpoint with a
non-existing `UUID`). The `option` method, provided by the `OptionalResponses` algebra
interface, takes a `Response[A]` description and
turns it into a `Response[Option[A]]`, mapping the `None` case
to a 404 (Not Found) response.

### Client and server implementations

The server implementation is very similar to what has been previously shown: we create a
type that inherits from `PublicEndpoints` and the relevant interpreters. The only new thing
is that we have to implement the abstract `uuidSegment: Segment[UUID]` member. In our
server interpreter based on Play, the `Segment` type is defined as follows:

~~~ scala src=../../../../play/server/src/main/scala/endpoints/play/server/Urls.scala#segment
~~~

The `decode` method is used when routing an incoming request, while the `encode` method is
used for “reverse routing” (ie to generate (valid) URLs of endpoints).

The implementation of the `uuidSegment` member is straightforward:

~~~ scala src=../../../examples/cqrs/public-server/src/main/scala/cqrs/publicserver/PublicServer.scala#segment-uuid
~~~

The client implementation is also straightforward: we create a type that inherits from
`PublicEndpoints` and the relevant interpreters. In our case, we use an interpreter that
derives a client performing `XMLHttpRequest`s to invoke the endpoints.

Again, we have to implement the `uuidSegment` member. In our interpreter the `Segment` type
is defined as follows:

~~~ scala src=../../../../xhr/client/src/main/scala/endpoints/xhr/Urls.scala#segment
~~~

Thus, we define the `uuidSegment` like so:

~~~ scala src=../../../examples/cqrs/web-client/src/main/scala/cqrs/webclient/PublicEndpoints.scala#segment-uuid
~~~

Finally, here is an example of invocation of the `listMeters` endpoint from the Scala.js client:

~~~ scala src=../../../examples/cqrs/web-client/src/main/scala/cqrs/webclient/Main.scala#list-meters-invocation
~~~

### Summary

In this section we have seen that deriving a Scala.js client works the same way as deriving a JVM
client.

We have seen that the endpoints library provides a minimal infrastructure that is designed to
be extended according to application-specific needs. In our case we saw how the `OptionalResponses`
introduced a new method for describing responses such that empty responses are mapped to
a 404 (Not Found) response. We also saw how support for custom data types (e.g. `UUID`) can
be introduced.

## Deriving an OpenAPI definition file from a service description

The [OpenAPI](https://www.openapis.org/) initiative standardizes a description format that
can be processed by tools like [swagger-ui](https://swagger.io/swagger-ui/) or
[Amazon API Gateway](http://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-import-api.html).

### Documented service description

The OpenAPI format requires richer service descriptions than what we currently have. For instance,
each response must have a human readable description. Because of such differences, we use
different algebra interfaces to define our service description. These algebra interfaces
are provided by the `endpoints-openapi` artifact:

~~~ mermaid
graph BT
  web-client -.-> interpreter2["endpoints-xhr-client-circe"]
  web-client -.-> public-endpoints
  web-client --> public-server
  public-server -.-> public-endpoints
  public-endpoints -.-> openapi["endpoints-openapi-circe"]
  public-server -.-> interpreter["endpoints-play-server-circe"]
  style interpreter fill:#eee;
  style interpreter2 fill:#eee;
  style openapi fill:#eee;
~~~

The “documented” description of the public endpoints looks like the following:

~~~ scala src=../../../examples/cqrs/public-endpoints/src/main/scala/cqrs/publicserver/documented/PublicEndpoints.scala#public-endpoints
~~~

A first difference with the previous definition of `PublicEndpoints` is that now we
import our algebra interfaces from the
[endpoints.documented.algebra](api:endpoints.documented.algebra.package) package instead
of `endpoints.algebra`. Though the name of the package is different, the names of the traits
are the same, and their methods are very similar but sometimes take additional parameters. Let’s
detail them:

- Responses now have a human readable documentation. For instance, the documentation for the
  `listMeters` endpoint response is “All the meters”,

~~~ scala src=../../../examples/cqrs/public-endpoints/src/main/scala/cqrs/publicserver/documented/PublicEndpoints.scala#list-meters
~~~

- Path parameters have an associated identifier. This one is used as a placeholder in the
  OpenAPI file. For instance, the identifier of the meter id, in the `getMeter` endpoint,
  is “id”. Consequently, the OpenAPI file definition shows the following path template
  for this endpoint: `/meters/{id}`.

~~~ scala src=../../../examples/cqrs/public-endpoints/src/main/scala/cqrs/publicserver/documented/PublicEndpoints.scala#meter-id
~~~

The `endpoints.documented.algebra` package contains algebra interface definitions that have
the same name and same methods as those that are in the `endpoints.algebra` package, but their
methods sometimes take additional parameters carrying documentation information. Thus, if you
want to turn a service description into a _documented_ service description, all you have to do
is to change one import and supply the missing parameters here and there.

### Deriving an OpenAPI file definition from a documented service description

To derive an OpenAPI file definition from our endpoint descriptions we use
the interpreters defined in the
[endpoints.documented.openapi](api:endpoints.documented.openapi.package) package:

~~~ scala src=../../../examples/cqrs/public-server/src/main/scala/cqrs/publicserver/documented/PublicEndpointsDocumentation.scala#documentation
~~~

Here, the
[openApi](api:endpoints.documented.openapi.Endpoints@openApi(info:endpoints.documented.openapi.Info)(endpoints:Endpoints.this.DocumentedEndpoint*):endpoints.documented.openapi.OpenApi)
method generates an abstract [OpenApi](api:endpoints.documented.openapi.OpenApi) model, which
can eventually be serialized in JSON.

### Deriving clients and servers from a documented service description

Since our documented endpoints are not defined by the algebra interfaces provided in the
`endpoints.algebra` package, we can not directly apply the interpreters introduced in the previous
sections. We have to use *delegation* to apply them.

For instance, here is the beginning of the `PublicServer` class definition, which applies
interpreters of the `endpoints.play.server` package to the `documented.PublicEndpoints`:

~~~ scala src=../../../examples/cqrs/public-server/src/main/scala/cqrs/publicserver/documented/PublicServer.scala#delegate-interpreter
~~~

Here is the definition of the client interpreter:

~~~ scala src=../../../examples/cqrs/web-client/src/main/scala/cqrs/webclient/documented/PublicEndpoints.scala
~~~

### Summary

To generate an OpenAPI definition from your service description this one
must be written using a different set of algebra interfaces, which live
in the `endpoints.documented.algebra` package.

You can then derive an OpenAPI definition from your documented endpoints
by applying the interpreters defined in the `endpoints.documented.openapi`
package.

None of the “documented” and “non-documented” algebra interfaces is a
subtype of each other, but “documented” interpreters can delegate to
“non-documented” ones.

## HATEOS

TODO.
