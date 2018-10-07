# Overview

The central idea of the *endpoints* library is that you first describe your communication endpoints
and then the library provides you:

- a server implementation decoding requests and building responses,
- a client implementation building requests and decoding responses,
- a machine readable documentation (OpenAPI document).

## Description of the HTTP endpoints

As an example, here is a `CounterEndpoints` trait describing two endpoints, one for getting a counter value and one for
incrementing it:

~~~ scala src=../../../examples/overview/endpoints/src/main/scala/overview/CounterEndpoints.scala#relevant-code
~~~

(Note that the example above uses Circe's `@JsonCodec` macro annotation, so you’ll need to include the Macro Paradise
compiler plugin to your [`build.sbt` file](https://github.com/julienrf/endpoints/blob/ca58dabbc544dd28464db1aa0eb4cf72f59489d0/documentation/build.sbt#L92))

## Client implementation

A client implementation of the endpoints can be obtained by mixing so-called “interpreters” to the `CounterEndpoints`
trait defined above. In this example, we get a JavaScript (Scala.js) client that uses `XMLHttpRequest` under
the hood:

~~~ scala src=../../../examples/overview/client/src/main/scala/overview/CounterClient.scala#relevant-code
~~~

And then, the `CounterClient` object can be used as follows:

~~~ scala src=../../../examples/overview/client/src/main/scala/overview/Usage.scala#current-value
~~~

And also:

~~~ scala src=../../../examples/overview/client/src/main/scala/overview/Usage.scala#increment
~~~

As you can see, invoking an endpoint is as easy as calling a method on the `CounterClient` object.
The *endpoints* library then builds an HTTP request (according to the endpoint description), sends
it to the server, and eventually decodes the HTTP response (according to the endpoint description).

## Server implementation

Similarly, a server implementation of the endpoints can be obtained by mixing the appropriate
interpreters to the `CounterEndpoints` trait. In this example, we get a JVM server that uses
Play framework under the hood:

~~~ scala src=../../../examples/overview/server/src/main/scala/overview/CounterServer.scala#relevant-code
~~~

The `CounterServer.routes` value produced by the *endpoints* library is a `Routes` value directly
usable by Play framework. To get an executable Web server we can setup a “main” like the following:

~~~ scala src=../../../examples/overview/server/src/main/scala/overview/Main.scala#relevant-code
~~~

The routes implementations provided by *endpoints* decode the incoming HTTP requests, call the corresponding logic
(here, incrementing the counter or getting its current value), and build the HTTP responses.

## Documentation generation

We conclude this overview by showing how to generate documentation for the endpoints, again by mixing the appropriate
interpreters:

~~~ scala src=../../../examples/overview/documentation/src/main/scala/overview/CounterDocumentation.scala#relevant-code
~~~

This code defines a `CounterDocumentation` object with an `api` member containing an OpenAPI object documenting
the `currentValue` and `increment` endpoints.

We can then render the documentation, for instance in JSON:

~~~ scala src=../../../examples/overview/documentation/src/main/scala/overview/CounterDocumentation.scala#export
~~~

This prints the following OpenAPI document:

~~~ javascript
{
  "openapi" : "3.0.0",
  "info" : {
    "title" : "API to manipulate a counter",
    "version" : "1.0.0"
  },
  "paths" : {
    "/increment" : {
      "post" : {
        "responses" : {
          "200" : {
            "description" : ""
          }
        },
        "requestBody" : {
          "content" : {
            "application/json" : {}
          }
        }
      }
    },
    "/current-value" : {
      "get" : {
        "responses" : {
          "200" : {
            "description" : "",
            "content" : {
              "application/json" : {}
            }
          }
        }
      }
    }
  }
}
~~~ 
