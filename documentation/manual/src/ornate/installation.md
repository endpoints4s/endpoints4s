# Installation

## Project layout

The typical installation consists in having a multi-project build, with
a `client` project and a `server` project, both depending on a `shared`
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

## Dependencies

All the artifacts are published on maven central under the organization
name `org.julienrf`.

### Endpoint descriptions

Add the following dependencies to your `shared` sub-project:

~~~ scala expandVars=true
libraryDependencies ++= Seq(
  // core API
  "org.julienrf" %% "endpoints-algebra" % "{{version}}",
  // (optional) JSON serialization using circe.io
  "org.julienrf" %% "endpoints-algebra-circe" % "{{version}}"
)
~~~

#### Documented endpoint descriptions

If you want to generate an [OpenAPI](https://www.openapis.org/) definition file
for your endpoint descriptions, add the following dependency to the `shared`
sub-project:

~~~ scala expandVars=true
libraryDependencies ++= Seq(
  // core API
  "org.julienrf" %% "endpoints-openapi" % "{{version}}",
  // (optional) JSON serialization using circe.io
  "org.julienrf" %% "endpoints-algebra-circe" % "{{version}}",
  // (optional) generic derivation of JSON schemas
  "org.julienrf" %% "endpoints-json-schema-generic" % "{{version}}"
)
~~~

### Client interpreters

#### Scala.js client using native `XMLHttpRequest`s

Add the following dependencies to your `client` sub-project:

~~~ scala expandVars=true
  // client based on JavaScript’s XMLHttpRequest
  "org.julienrf" %%% "endpoints-xhr-client" % "{{version}}",
  // (optional) JSON serialization using circe.io
  "org.julienrf" %%% "endpoints-xhr-client-circe" % "{{version}}",
  // (optional) uses faithful’s `Future`
  "org.julienrf" %%% "endpoints-xhr-client-faithful" % "{{version}}"
)
~~~

#### Client based on Play framework (JVM only)

Add the following dependencies to your `client` sub-project:

~~~ scala expandVars=true
libraryDependencies ++= Seq(
  // client based on Play framework
  "org.julienrf" %% "endpoints-play-client" % "{{version}}",
  // (optional) JSON serialization using circe.io
  "org.julienrf" %% "endpoints-play-client-circe" % "{{version}}"
)
~~~

#### Client backed by scalaj-http

~~~ scala expandVars=true
libraryDependencies ++= Seq(
  // client based on scalaj-http
  "org.julienrf" %% "endpoints-scalaj-client" % "{{version}}",
  // (optional) JSON serialization using circe.io
  "org.julienrf" %% "endpoints-scalaj-client-circe" % "{{version}}"
)
~~~

#### Client backed by akka-http (JVM only)

~~~ scala expandVars=true
libraryDependencies ++= Seq(
  // client based on akka-http
  "org.julienrf" %% "endpoints-akka-http-client" % "{{version}}",
  // (optional) JSON serialization using circe.io
  "org.julienrf" %% "endpoints-akka-http-client-circe" % "{{version}}"
)
~~~

### Server interpreters

#### Server based on Play framework

Add the following dependencies to your `server` sub-project:

~~~ scala expandVars=true
libraryDependencies ++= Seq(
  // server based on play framework
  "org.julienrf" %% "endpoints-play-server" % "{{version}}",
  // (optional) JSON serialization using circe.io
  "org.julienrf" %% "endpoints-play-server-circe" % "{{version}}"
)
~~~

#### Server based on Akka-HTTP

Add the following dependencies to your `server` sub-project:

~~~ scala expandVars=true
libraryDependencies ++= Seq(
  // server based on akka-http
  "org.julienrf" %% "endpoints-akka-http-server" % "{{version}}",
  // (optional) JSON serialization using circe.io
  "org.julienrf" %% "endpoints-akka-http-server-circe" % "{{version}}"
)
~~~
