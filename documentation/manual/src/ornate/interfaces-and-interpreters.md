# Algebra interfaces and their interpreters

Because of the modular design of _endpoints_ it might be difficult to find which
modules are relevant for a given use case. This page aims to make it easier to
navigate through the algebra interfaces hierarchy.

## Naming conventions

- algebra interfaces are defined as traits in the `endpoints.algebra`
  package ;
    - e.g. the [`Urls`](api:endpoints.algebra.Urls) trait defines an algebra
      interface for describing URLs.
- algebra interfaces dependencies can be found in their super types ;
- interpreters are traits that have the same name of the corresponding
  algebra interface (they can also be found by looking at the “known
  subclasses” of an algebra interface, in the Scaladoc) ;
- compatible interpreters are in the same package ;
  - e.g. the [`endpoints.play.client`](api:endpoints.play.client.package)
    package provides interpreters that turn endpoints descriptions into
    clients using [play-ws](https://github.com/playframework/play-ws) under
    the hood.

## Algebra interfaces

| Name | Description | Artifact id |
|---|---|---|
|[`Assets`](api:endpoints.algebra.Assets)|Asset segments, endpoints serving fingerprinted assets|[endpoints-algebra](https://index.scala-lang.org/julienrf/endpoints/endpoints-algebra)|
|[`BasicAuthentication`](api:endpoints.algebra.BasicAuthentication)|HTTP Basic authentication|[endpoints-algebra](https://index.scala-lang.org/julienrf/endpoints/endpoints-algebra)|
|[`Endpoints`](api:endpoints.algebra.Endpoints)|HTTP endpoints (main algebra interface)|[endpoints-algebra](https://index.scala-lang.org/julienrf/endpoints/endpoints-algebra)|
|[`JsonEntities`](api:endpoints.algebra.JsonEntities)|JSON request and response entities|[endpoints-algebra](https://index.scala-lang.org/julienrf/endpoints/endpoints-algebra)|
|[`OptionalResponses`](api:endpoints.algebra.OptionalResponses)|Optional response entities|[endpoints-algebra](https://index.scala-lang.org/julienrf/endpoints/endpoints-algebra)|
|[`circe.JsonEntitiesFromCodec`](api:endpoints.algebra.circe.JsonEntitiesFromCodec)|JSON request and response entities using Circe codecs|[endpoints-algebra-circe](https://index.scala-lang.org/julienrf/endpoints/endpoints-algebra-circe)|
|[`playjson.JsonEntitiesFromCodec`](api:endpoints.algebra.playjson.JsonEntitiesFromCodec)|JSON request and response entities using Play JSON codecs|[endpoints-algebra-playjson](https://index.scala-lang.org/julienrf/endpoints/endpoints-algebra-playjson)|

**Note**: A similar but slightly different set of algebra interfaces can be used to define endpoint
descriptions containing documentation. These algebra interfaces have the same names as those
described in above table, but live in the
[endpoints.documented.algebra](api:endpoints.documented.algebra.package) package and are provided
by the [endpoints-openapi](https://index.scala-lang.org/julienrf/endpoints/endpoints-openapi)
artifact. This artifact also provides its interpreters (in the
[endpoints.documented.openapi](api:endpoints.documented.openapi.package) and
[endpoints.documented.delegate](api:endpoints.documented.delegate.package) packages).

## Interpreters

| Artifact id | Description |
|---|---|
|[endpoints-akka-http-server](https://index.scala-lang.org/julienrf/endpoints/endpoints-akka-http-server)|Server backed by Akka-http|
|[endpoints-akka-http-client](https://index.scala-lang.org/julienrf/endpoints/endpoints-akka-client)|Client backed by Akka-http|
|[endpoints-play-client](https://index.scala-lang.org/julienrf/endpoints/endpoints-play-client)|Client backed by Play framework|
|[endpoints-play-server](https://index.scala-lang.org/julienrf/endpoints/endpoints-play-server)|Server backed by Play framework|
|[endpoints-play-server-circe](https://index.scala-lang.org/julienrf/endpoints/endpoints-play-server-circe)|Interpreter for `JsonEntities` that uses Circe to decode requests and encode responses, and which is compatible with `endpoints-play-server`|
|[endpoints-xhr-client](https://index.scala-lang.org/julienrf/endpoints/endpoints-xhr-client)|Scala.js clients using `XMLHttpRequest` and returning responses in `Future` or `js.Thenable` values|
|[endpoints-xhr-client-circe](https://index.scala-lang.org/julienrf/endpoints/endpoints-xhr-client-circe)|Interpreter for `JsonEntities` that uses Circe to encode requests and decode responses, and which is compatible with `endpoints-xhr-client`|
|[endpoints-xhr-client-faithful](https://index.scala-lang.org/julienrf/endpoints/endpoints-xhr-client-faithful)|Scala.js client returning responses [faithful](https://github.com/julienrf/faithful)’s `Future` values|
|[endpoints-scalaj-client](https://index.scala-lang.org/julienrf/endpoints/endpoints-scalaj-client)|JVM client backed by [scalaj-http](https://github.com/scalaj/scalaj-http)|
