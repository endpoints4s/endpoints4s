# Tutorial

This tutorial introduces the main features of the endpoints library
based on a complete application.

## Introduction

This section describes the application domain and architecture.
For the sake of brevity, this tutorial only shows the parts of the code
that are rely on the endpoints library. The complete
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

Second, the `public-server` itself is broken down into smaller pieces. It follows a
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

## Defining a communication protocol

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
they will use.

We achieve this with plain Scala expressions describing all the characteristics
(verb, URL, entities, etc.) of each HTTP endpoint.

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

First, we import the [`Endpoints`](api:endpoints.play.routing.Endpoints) and
[`CirceEntities`](api:endpoints.play.routing.CirceEntities) *interpreters* from the
`endpoints.play.routing` package. Interpreters always have the same name as the
algebra interface they implement, but they are not located in the `endpoints.algebra`
package. The `endpoints.play.routing` package contains interpreters that rely
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

As you can see, the `play.routing` interpreter takes care of decoding HTTP requests and
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

~~~ scala src=../../../play-client/src/main/scala/endpoints/play/client/Endpoints.scala#concrete-carrier-type
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

## Queries service description

TODO.

## Web client

TODO.