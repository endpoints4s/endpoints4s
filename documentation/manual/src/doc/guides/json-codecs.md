# JSON codecs

The [JSON entities algebras](/algebras/json-entities.md) show that several options
are available to define endpoints having JSON entities. In this guide, we show
which options should be used in which case, and which interpreters are compatible
with which algebras.

## Use codecs at the algebra level

The most general algebra for defining JSON entities uses separate
types for request and response entities:

~~~ scala src=../../../../../algebras/algebra/src/main/scala/endpoints/algebra/JsonEntities.scala#request-response-types
~~~

However, in case your endpoint definitions are interpreted in multiple ways these types
should be aligned so that requests and responses are consistently encoded, decoded and
documented by client, server and documentation interpreters. A consequence of this is that an
implicit instance of such codecs has to be available for your data types at the
definition-site of your endpoints.

The next sections show how to use the two families of “codec”-based `JsonEntities` algebra
specializations: `JsonEntitiesFromCodec` and `JsonSchemaEntities`.

## `JsonEntitiesFromCodec`

### Algebra

In case you don’t need to document the JSON schemas of your request and response entities, the
`JsonEntitiesFromCodec` family of algebras is the preferred approach. These algebras fix both
the `JsonRequest` and `JsonResponse` types to a same (abstract) `JsonCodec` type:

~~~ scala src=../../../../../algebras/algebra/src/main/scala/endpoints/algebra/JsonEntities.scala#json-codec-type
~~~

Generally, you want to use a `JsonEntitiesFromCodec` algebra that fixes this `JsonCodec` type to
a concrete type. An example is `endpoints.algebra.playjson.JsonEntitiesFromCodec`, which
aligns the `JsonCodec` type with Play’s `Format` type:

~~~ scala src=../../../../../algebras/algebra-playjson/src/main/scala/endpoints/algebra/playjson/JsonEntitiesFromCodec.scala#type-carrier
~~~

The [JSON entities algebra](/algebras/json-entities.md#jsonentitiesfromcodec) documentation page
shows the list of `JsonEntitiesFromCodec` algebras that fixes the `JsonCodec` type to a concrete type.

### Interpreters

To interpret endpoints defined with such algebras, apply any interpreter named `JsonEntitiesFromCodec`
that matches your [family](/algebras-and-interpreters.md#interpreters) of interpreters. For instance,
if you use interpreters from the `endpoints.xhr` package (ie. the Scala.js web interpreters), you
should use the `endpoints.xhr.JsonEntitiesFromCodec` interpreter.

## `JsonSchemaEntities`

### Algebra

The `endpoints.algebra.JsonSchemaEntities` algebra allows you to document the JSON schema of request
and response entities (when you apply a documentation interpreter to endpoints defined with this
algebra). Both the `JsonRequest` and `JsonResponse` types are fixed to the
`JsonSchema` type provided by the [JsonSchemas](/algebras/json-schemas.md) algebra:

~~~ scala src=../../../../../algebras/algebra/src/main/scala/endpoints/algebra/JsonSchemaEntities.scala#type-carrier
~~~

### Interpreters

To interpret endpoints defined with this algebra, pick an interpreter that matches your
[family](/algebras-and-interpreters.md#interpreters) of interpreters and, if relevant,
the underlying JSON library to use. For instance, the
`endpoints.play.server.circe.JsonSchemaEntities` trait is a server interpreter based
on Play framework that uses the circe JSON library.

