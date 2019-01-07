# JSON entities

## `JsonEntities`

This algebra provides vocabulary to define request and response entities
containing JSON documents.

~~~ scala expandVars=true
"org.julienrf" %% "endpoints-algebra" % "{{version}}"
~~~

[API documentation](api:endpoints.algebra.JsonEntities)

The module enriches the `Endpoints` algebra with new constructors
for request and response entities. For instance, here is how to
define an endpoint taking in its request entity a JSON document
for creating an user, and returning in its response entity the
created user:

~~~ scala src=../../../../../algebras/algebra/src/test/scala/endpoints/algebra/JsonEntitiesDocs.scala#json-entities
~~~

The `jsonRequest[A]` constructor defines a JSON request entity containing
a value of type `A`, provided that there exists an implicit `JsonRequest[A]`
instance. Similarly, the `jsonResponse[A]` constructor defines a JSON
response entity containing a value of type `A` provided that there exists an
implicit `JsonResponse[A]` instance.

The `JsonRequest[A]` and `JsonResponse[A]` types are kept abstract in this
algebra. They mean that a value of type `A` can be serialized in a
JSON request or a JSON response, respectively.

Typically, client interpreters fix the `JsonRequest[A]` type to
be a JSON encoder for `A` and the `JsonResponse[A]` type to be
a JSON decoder for `A`. Similarly, server interpreters fix the
`JsonRequest[A]` type to be a JSON decoder for `A`, and the
`JsonResponse[A]` type to be a JSON encoder for `A`. The documentation
interpreters fix these types to be JSON schemas for `A`.

The remaining sections document some concrete types given by algebras
extending `JsonEntities`. You might also be interested in looking at
the [JSON codecs guide](/guides/json-codecs.md), which explains which
families of algebras and interpreters you should use together.

## `JsonSchemaEntities`

~~~ scala expandVars=true
"org.julienrf" %% "endpoints-algebra" % "{{version}}"
~~~

[API documentation](api:endpoints.algebra.JsonSchemaEntities)

This algebra merges the `JsonEntities` algebra and the
[`JsonSchemas` algebra](json-schemas.md) and aligns both the
`JsonRequest[A]` and `JsonResponse[A]` types to be `JsonSchema[A]`. This means that
JSON schemas defined by using the `JsonSchemas` algebra can be used in request and
responses entities.

## `JsonEntitiesFromCodec`

~~~ scala expandVars=true
"org.julienrf" %% "endpoints-algebra" % "{{version}}"
~~~

[API documentation](api:endpoints.algebra.JsonEntitiesFromCodec)

This algebra fixes both the `JsonRequest[A]` and `JsonResponse[A]` types to the same
codec type able to both encode and decode `A` values into and from JSON documents. By
using the same codec type for both types ensures that the encoding and decoding
are consistent.

This trait introduces an abstract `JsonCodec[A]` type, which is fixed by more concrete
interpreters such as
[playjson.JsonEntitiesFromCodec](api:endpoints.algebra.playjson.JsonEntitiesFromCodec)
or [circe.JsonEntitiesFromCodec](api:endpoints.algebra.circe.JsonEntitiesFromCodec).
There interpreters are provided by the following dependencies:

~~~ scala expandVars=true
// Provides endpoints.algebra.circe.JsonEntitiesFromCodec
"org.julienrf" %% "endpoints-algebra-circe" % "{{version}}"
// Provides endpoints.algebra.playjson.JsonEntitiesFromCodec
"org.julienrf" %% "endpoints-algebra-playjson" % "{{version}}"
~~~
