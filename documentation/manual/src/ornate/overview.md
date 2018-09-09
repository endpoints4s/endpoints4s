# Overview

Typically, a project is broken down into several sub-projects:

~~~ mermaid
graph BT
  endpoints
  server -.-> endpoints
  client -.-> endpoints
~~~

The `endpoints` sub-project contains the *description* of the communication
protocol. The `server` sub-project *implements* this communication protocol.
The `client` sub-project *uses* the protocol to communicate with the `server`.

## Description of the HTTP endpoints

Let’s define a first artifact, cross-compiled for Scala.js, and containing a description of the
endpoints of a web service.

~~~ scala src=../../../examples/overview/endpoints/src/main/scala/overview/CounterEndpoints.scala#relevant-code
~~~

Note that the example above uses Circe's `@JsonCodec` macro annotation, so you’ll also need to include the Macro Paradise compiler plugin in your `build.sbt` file.

```
addCompilerPlugin(
  "org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full
)
```

## JavaScript (Scala.js) client

The following code, located in the `client` sub-project, defines a Scala.js
client for the web service.

~~~ scala src=../../../examples/overview/client/src/main/scala/overview/CounterClient.scala#relevant-code
~~~

And then, this client can be used as follows:

~~~ scala src=../../../examples/overview/client/src/main/scala/overview/Usage.scala#current-value
~~~

And also:

~~~ scala src=../../../examples/overview/client/src/main/scala/overview/Usage.scala#increment
~~~

## Service implementation (backed by Play framework)

The following code, located in the `server` sub-project, defines the implementation of
the web service.

~~~ scala src=../../../examples/overview/server/src/main/scala/overview/CounterServer.scala#relevant-code
~~~

The `CounterServer.routes` value is just a `play.api.routing.Router.Routes`.
To get an executable Web server we need to setup a “main” like the following:

~~~ scala src=../../../examples/overview/server/src/main/scala/overview/Main.scala#relevant-code
~~~

## What else?

You can also get a Scala/JVM client (which uses `play-ws` under the hood) as follows:

~~~ scala src=../../../examples/overview/play-client/src/main/scala/overview/CounterClient.scala#relevant-code
~~~

Thus, you can distribute a (fully working) JVM client, which is independent of your implementation.
