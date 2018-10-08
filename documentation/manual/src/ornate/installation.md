# Installation

## Project layout

The typical installation consists in having a multi-project build, with
a `client` project and a `server` project both depending on a `shared`
project:

~~~ mermaid
graph BT
  shared
  server -.-> shared
  client -.-> shared
~~~

The `shared` project contains the *description* of the communication
protocol. The `server` project *implements* this communication protocol.
The `client` project *uses* the protocol to communicate with the `server`.

This translates to the following `build.sbt` configuration:

~~~ scala
val shared = project

val client = project.dependsOn(shared)

val server = project.dependsOn(shared)
~~~

Then, add a dependency to the *endpoints* algebra interfaces to the
`shared` project, add dependencies to client interpreters to the
`client` project, and add dependencies to server interpreters to the
`server` project, as shown in the next section.

## Dependencies

All the artifacts are published on maven central under the organization
name `org.julienrf`. See the
[interfaces and interpreters](interfaces-and-interpreters.md) page
for more details on what each specific artifact provides.

### Endpoint descriptions

Add the following dependencies to your `shared` project:

~~~ scala expandVars=true
libraryDependencies ++= Seq(
  // core API
  "org.julienrf" %% "endpoints-algebra" % "{{version}}",
  // (optional) JSON serialization using circe.io
  "org.julienrf" %% "endpoints-algebra-circe" % "{{version}}",
  // (optional) JSON serialization using play-json
  "org.julienrf" %% "endpoints-algebra-playjson" % "{{version}}"
)
~~~

### Client interpreters

#### Scala.js client using native `XMLHttpRequest`s

Add the following dependencies to your `client` project:

~~~ scala expandVars=true
libraryDependencies ++= Seq(
  // client based on JavaScript’s XMLHttpRequest
  "org.julienrf" %%% "endpoints-xhr-client" % "{{version}}",
  // (optional) JSON serialization using circe.io
  "org.julienrf" %%% "endpoints-xhr-client-circe" % "{{version}}",
  // (optional) uses faithful’s `Future`
  "org.julienrf" %%% "endpoints-xhr-client-faithful" % "{{version}}"
)
~~~

#### Client based on Play framework (JVM only)

Add the following dependency to your `client` project:

~~~ scala expandVars=true
libraryDependencies ++= Seq(
  "org.julienrf" %% "endpoints-play-client" % "{{version}}"
)
~~~

#### Client backed by scalaj-http (JVM only)

Add the following dependency to your `client` project:

~~~ scala expandVars=true
libraryDependencies ++= Seq(
  "org.julienrf" %% "endpoints-scalaj-client" % "{{version}}"
)
~~~

#### Client backed by akka-http (JVM only)

Add the following dependency to your `client` project:

~~~ scala expandVars=true
libraryDependencies ++= Seq(
  "org.julienrf" %% "endpoints-akka-http-client" % "{{version}}"
)
~~~

#### Client backed by sttp (JVM only)

Add the following dependency to your `client` project:

~~~ scala expandVars=true
libraryDependencies ++= Seq(
  "org.julienrf" %% "endpoints-sttp-client" % "{{version}}"
)
~~~

### Server interpreters

#### Server based on Play framework

Add the following dependencies to your `server` project:

~~~ scala expandVars=true
libraryDependencies ++= Seq(
  // server interpreter based on Play framework
  "org.julienrf" %% "endpoints-play-server" % "{{version}}",
  // (optional) JSON serialization using circe.io
  "org.julienrf" %% "endpoints-play-server-circe" % "{{version}}",
  // (optional) JSON serialization using play-json
  "org.julienrf" %% "endpoints-play-server-playjson" % "{{version}}"
)
~~~

#### Server based on Akka-HTTP

Add the following dependency to your `server` project:

~~~ scala expandVars=true
libraryDependencies ++= Seq(
  "org.julienrf" %% "endpoints-akka-http-server" % "{{version}}"
)
~~~

### Documentation interpreters

To generate an [OpenAPI](https://www.openapis.org/) document for your endpoint
descriptions, add the following dependency to the `server` project:

~~~ scala expandVars=true
libraryDependencies ++= Seq(
  "org.julienrf" %% "endpoints-openapi" % "{{version}}"
)
~~~
