# Tutorial — OpenAPI

This tutorial shows how to produce an [OpenAPI](https://www.openapis.org/) documentation from
an API description.

## Introduction

The OpenAPI initiative standardizes a description format that can be processed by tools like
[swagger-ui](https://swagger.io/swagger-ui/) or
[Amazon API Gateway](http://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-import-api.html).

In this tutorial we show how to implement a simple application that manages a counter, and
how to produce an OpenAPI documentation for this application. The complete
source code fits in a single file of 150 lines (with lots of comments) available
[here](https://github.com/julienrf/endpoints/tree/master/documentation/examples/documented/src/main/scala/counter/Counter.scala).

The application is defined in a single sbt project with the following dependencies:

~~~ mermaid
graph BT
  counter -.-> endpoints-algebra-circe
  counter -.-> endpoints-json-schema-generic
  counter -.-> endpoints-play-server-circe
  style endpoints-circe fill:#eee;
  style endpoints-play-server-circe fill:#eee;
  style endpoints-json-schema-generic fill:#eee;
~~~

The `endpoints-algebra-circe` dependency provides an algebra interface to describe
HTTP endpoints that can produce JSON encoders and decoders for circe as well as OpenAPI documentation.

The `endpoints-json-schema-generic` dependency provides generic JSON schema
descriptions for algebraic data types.

The `endpoints-play-server-circe` dependency provides a server interpreter that
uses Play framework and circe under the hood.

We break down the application into the following components:

~~~ mermaid
graph BT
  CounterEndpoints-.->endpoints-json-schema-generic
  CounterServer-.->endpoints-play-server-circe
  DocumentationServer-.->endpoints-play-server-circe
  subgraph counter
    CounterDocumentation-->CounterEndpoints
    CounterServer-->CounterEndpoints
    DocumentationServer-.->CounterDocumentation
  end
  style endpoints-play-server-circe fill:#eee;
  style endpoints-json-schema-generic fill:#eee;
~~~

`CounterEndpoints` contains the description of the HTTP endpoints of the application,
`CounterServer` interprets them as an HTTP server, `CounterDocumentation` interprets
them as an OpenAPI document, and `DocumentationServer` contains additional endpoints
that serve the OpenAPI documentation.

The business domain of our application is defined by the following algebraic data
types:

~~~ scala src=../../../examples/documented/src/main/scala/counter/Counter.scala#domain
~~~

## Description of the HTTP endpoints

We want to define two endpoints: one for querying the counter value and one for
modifying it.

~~~ scala src=../../../examples/documented/src/main/scala/counter/Counter.scala#documented-endpoints
~~~

Note that we first import
[endpoints.algebra](api:endpoints.algebra.package). We use the same algebra for defining documentation
as we would use for definig protocols for client/server imterpreatation.

For instance, the `counterJson` value describes an HTTP response whose JSON entity contains
“The counter current value”.

In summary, the `endpoints.algebra` package contains algebra interface definitions
with methods that sometimes take additional parameters carrying documentation information.
Thus, if you want to turn a service description into a _documented_ service description, all
you have to do is to supply the missing parameters here and there.

The two last definitions of the above code snippet define the JSON schema of our `Counter`
and `Operation` data types. A “schema” describes the structure of a data type: in case
of a case class it describes its fields (their name and type), and in case of a sealed
trait it describes its alternative constructors.

These schemas will be used later on by the server, to decode incoming
request entities. The OpenAPI documentation will also use them, to describe the expected
structure of the endpoint entities. Because this documentation and the JSON codecs
are based on the same schema, they are always consistent together.

In our case, our schemas are generically
derived from the structure of our case classes and sealed traits, so that we don’t have
to repeat it.

## Deriving an OpenAPI file definition from a documented service description

To derive an OpenAPI file definition from our endpoint descriptions we use
the interpreters defined in the
[endpoints.openapi](api:endpoints.openapi.package) package:

~~~ scala src=../../../examples/documented/src/main/scala/counter/Counter.scala#openapi
~~~

Here, the
[openApi](api:endpoints.openapi.Endpoints@openApi(info:endpoints.openapi.Info)(endpoints:Endpoints.this.DocumentedEndpoint*):endpoints.openapi.OpenApi)
method generates an abstract [OpenApi](api:endpoints.openapi.OpenApi) model, which
can eventually be serialized to JSON.

## Deriving an HTTP server from a documented service description

Since our documented endpoints are not defined by the algebra interfaces provided in the
`endpoints.algebra` package, we can not directly apply the interpreters introduced in the
other [tutorial](tutorial.md). But we can use *delegation* to apply them.

For instance, here is the beginning of our `CounterServer` class definition, which applies
interpreters of the `endpoints.play.server` package to the `CounterEndpoints`:

~~~ scala src=../../../examples/documented/src/main/scala/counter/Counter.scala#delegation
~~~

We first mix interpreters provided in the `delegate` package to our `CounterEndpoints`
trait, and then we define a value named `delegate`, which contains the interpreter
that matches algebra interfaces provided in the `endpoints.algebra` package. In our
case we use an interpreter based on Play framework.

## Business logic and JVM entry point

For the sake of completeness, here is how the business logic is implemented:

~~~ scala src=../../../examples/documented/src/main/scala/counter/Counter.scala#business-logic
~~~

There is nothing specific to “documented” endpoints here: we create Play routes
by attaching a business logic to each HTTP endpoint.

And here the JVM entry point, which starts the HTTP server:

~~~ scala src=../../../examples/documented/src/main/scala/counter/Counter.scala#entry-point
~~~

The `DocumentationServer` object contains both description and implementation of HTTP
endpoints needed to serve the documentation. The most important one is `documentation`,
which serves the OpenAPI document defined by the `CounterDocumentation` object.

Finally, we start a `NettyServer` and give it both the `CounterServer` and
`DocumentationServer` routes.

## Summary

To generate an OpenAPI definition from your service description it's enough to use
interpreters defined in `endpoints.openapi` package. Generated documentation can be enriched
by supplying additional information when creating the service description.