# Quick start

The central idea of endpoints4s is that you first define an abstract
description of your HTTP endpoints and then the library provides:

- a server implementation decoding requests and building responses,
- a client implementation building requests and decoding responses,
- a machine readable documentation (OpenAPI document).

## Project layout

The typical setup consists in a multi-project build, with
a `client` project and a `server` project both depending on a `shared`
project.

The `shared` project contains the *description* of the communication
protocol. The `server` project *implements* this communication protocol.
The `client` project *uses* the protocol to communicate with the `server`.

![](project-layout.svg)

## Dependencies

The `shared` project has to depend on so-called *algebras*, which provide the vocabulary
to describe the communication endpoints, and the `client` and `server` projects
have to depend on *interpreters*, which give a concrete meaning to the endpoint descriptions.
See the @ref[algebras and interpreters](algebras-and-interpreters.md) page for an exhaustive
list.

In this example you will use the following dependencies:

@@@vars
~~~ scala
val shared =
  crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Pure).settings(
    libraryDependencies ++= Seq(
      "org.endpoints4s" %%% "algebra" % "$version$",
      // optional, see explanation below
      "org.endpoints4s" %%% "json-schema-generic" % "$version$"
    )
  )

val sharedJS = shared.js
val sharedJVM = shared.jvm

val client =
  project.enablePlugins(ScalaJSPlugin).settings(
    libraryDependencies += "org.endpoints4s" %%% "xhr-client" % "$xhr-client-version$"
  ).dependsOn(sharedJS)

val server =
  project.settings(
    libraryDependencies ++= Seq(
      "org.endpoints4s" %% "akka-http-server" % "$akka-http-version$",
      "org.scala-stm" %% "scala-stm" % "0.8"
    )
  ).dependsOn(sharedJVM)
~~~
@@@

The `shared` project uses the
@ref[`json-schema-generic` module](algebras/json-schemas.md#generic-derivation-of-json-schemas-based-on-shapeless-)
in addition to the required algebra interface @ref[`algebra`](algebras/endpoints.md),
to define the communication endpoints and to automatically derive the
JSON schemas of the entities from their Scala type definitions.

The `client` project uses a @ref[Scala.js web](interpreters/scalajs-web.md) client interpreter.

Finally, the `server` project uses a server interpreter backed by @ref[Akka HTTP](interpreters/akka-http.md).
It also uses the scala-stm library for implementing the business logic.

## Description of the HTTP endpoints

In the `shared` project, define a `CounterEndpoints` trait describing two endpoints, one
for getting a counter value and one for incrementing it:

@@snip [CounterEndpoints.scala](/documentation/examples/quickstart/endpoints/src/main/scala/quickstart/CounterEndpoints.scala) { #relevant-code }

The `currentValue` and `increment` members define the endpoints for getting the counter
current value or incrementing it, as their names suggest. The `counterSchema` and
`incrementSchema` members define a JSON schema that will be used to serialize and
deserialize the request and response entities.

## Client implementation

A client implementation of the endpoints can be obtained by mixing so-called “interpreters” to the `CounterEndpoints`
trait defined above. In this example, you want to get a JavaScript (Scala.js) client that uses `XMLHttpRequest` under
the hood. Defines the following `CounterClient` object in the `client` project:

@@snip [CounterClient.scala](/documentation/examples/quickstart/client/src/main/scala/quickstart/CounterClient.scala) { #relevant-code }

And then, the `CounterClient` object can be used as follows:

@@snip [Usage.scala](/documentation/examples/quickstart/client/src/main/scala/quickstart/Usage.scala) { #current-value }

And also:

@@snip [Usage.scala](/documentation/examples/quickstart/client/src/main/scala/quickstart/Usage.scala) { #increment }

As you can see, invoking an endpoint consists of calling a function on the `CounterClient` object.
endpoints4s then builds an HTTP request (according to the endpoint description), sends
it to the server, and eventually decodes the HTTP response (according to the endpoint description).

## Server implementation

Similarly, a server implementation of the endpoints can be obtained by mixing the appropriate
interpreters to the `CounterEndpoints` trait. In this example, you want to get a JVM server
that uses Akka HTTP under the hood. Create the following `CounterServer` class in the
`server` project:


@@snip [CounterServer.scala](/documentation/examples/quickstart/server/src/main/scala/quickstart/CounterServer.scala) { #relevant-code }

The `routes` value produced by endpoints4s is a `Route` value directly
usable by Akka HTTP. The last section shows how to setup an Akka HTTP server that
uses these routes.

The routes implementations provided by endpoints4s decode the incoming HTTP requests, call the corresponding logic
(here, incrementing the counter or getting its current value), and build the HTTP responses.

## Documentation generation

You can also generate documentation for the endpoints,
again by mixing the appropriate interpreters. Create the following `CounterDocumentation`
object in the `server` project:

@@snip [CounterDocumentation.scala](/documentation/examples/quickstart/server/src/main/scala/quickstart/CounterDocumentation.scala) { #relevant-code }

This code defines a `CounterDocumentation` object with an `api` member containing an OpenAPI object documenting
the `currentValue` and `increment` endpoints.

## Running the application

Finally, to run your application you need to build a proper Akka HTTP server serving your routes.
Define the following `Main` object:

@@snip [Main.scala](/documentation/examples/quickstart/server/src/main/scala/quickstart/Main.scala) { #relevant-code }

You can then browse the
[http://localhost:8000/current-value](http://localhost:8000/current-value)
URL to query the counter value, or the
[http://localhost:8000/documentation.json](http://localhost:8000/current-value)
URL to get the generated OpenAPI documentation, which should look like
the following:

~~~ javascript
{
  "openapi": "3.0.0",
  "info": {
    "title": "API to manipulate a counter",
    "version": "1.0.0"
  },
  "components": {
    "schemas": {
      "quickstart.Counter": {
        "type": "object",
        "properties": {
          "value": {
            "format": "int32",
            "type": "integer"
          }
        },
        "required": ["value"]
      },
      "quickstart.Increment": {
        "type": "object",
        "properties": {
          "step": {
            "format": "int32",
            "type": "integer"
          }
        },
        "required": ["step"]
      },
      "endpoints.Errors": {
        "type": "array",
        "items": {
          "type": "string"
        }
      }
    }
  },
  "paths": {
    "/increment": {
      "post": {
        "requestBody": {
          "content": {
            "application/json": {
              "schema": {
                "$ref": "#/components/schemas/quickstart.Increment"
              }
            }
          }
        },
        "responses": {
          "400": {
            "description": "Client error",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/components/schemas/endpoints.Errors"
                }
              }
            }
          },
          "500": {
            "description": "Server error",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/components/schemas/endpoints.Errors"
                }
              }
            }
          },
          "200": {
            "description": ""
          }
        }
      }
    },
    "/current-value": {
      "get": {
        "responses": {
          "400": {
            "description": "Client error",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/components/schemas/endpoints.Errors"
                }
              }
            }
          },
          "500": {
            "description": "Server error",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/components/schemas/endpoints.Errors"
                }
              }
            }
          },
          "200": {
            "description": "",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/components/schemas/quickstart.Counter"
                }
              }
            }
          }
        }
      }
    }
  }
}
~~~ 

## Next Step

Learn about the @ref[design principles](design.md) of the _endpoints_ library.
