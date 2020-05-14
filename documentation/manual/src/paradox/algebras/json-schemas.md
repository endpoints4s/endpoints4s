# JSON Schemas

## `JsonSchemas`

This algebra provides vocabulary to define JSON schemas of data types.

@@@vars
~~~ scala
"org.julienrf" %% "endpoints-algebra-json-schema" % "$version$"
~~~
@@@

@scaladoc[API documentation](endpoints.algebra.JsonSchemas)

@@@note
This module is dependency-free, it can be used independently of *endpoints*
to define JSON schemas and interpret them as actual encoder, decoders or
documentation.
@@@

The algebra introduces the concept of `JsonSchema[A]`: a JSON schema for a type `A`.

### Basic types and record types

The trait provides some predefined JSON schemas (for `String`, `Int`, `Boolean`, `Seq`, etc.)
and ways to combine them together to build more complex schemas.

For instance, given the following `Rectangle` data type:

@@snip [JsonSchemasDocs.scala](/json-schema/json-schema/src/test/scala/endpoints/algebra/JsonSchemasDocs.scala) { #record-type }

We can represent instances of `Rectangle` in JSON with a JSON object having properties corresponding
to the case class fields. A JSON schema for such objects would be defined as follows:

@@snip [JsonSchemasDocs.scala](/json-schema/json-schema/src/test/scala/endpoints/algebra/JsonSchemasDocs.scala) { #record-schema }

The `field` constructor defines a JSON object schema with one field of the given
type and name (and an optional text documentation). A similar constructor, `optField`,
defines an optional field in a JSON object.

The return type of `rectangleSchema` is declared to be `JsonSchema[Rectangle]`, but we could have
used a more specific type: `Record[Rectangle]`. This subtype of `JsonSchema[Rectangle]` provides
additional operations such as `zip` or `tagged` (see the next section).

In the above example, we actually define two JSON object schemas (one for the `width` field,
of type `Record[Double]`, and one for the `height` field, of type `Record[Double]`),
and then we combine them into a single JSON object schema by using the `zip` operation. Finally, we call the `xmap` operation
to turn the `Record[(Double, Double)]` value returned by the `zip` operation into
a `Record[Rectangle]`.

### Sum types (sealed traits)

It is also possible to define schemas for sum types. Consider the following type definition,
defining a `Shape`, which can be either a `Circle` or a `Rectangle`:

@@snip [JsonSchemasDocs.scala](/json-schema/json-schema/src/test/scala/endpoints/algebra/JsonSchemasDocs.scala) { #sum-type }

A possible JSON schema for this data type consists in using a JSON object with a discriminator
field indicating whether the `Shape` is a `Rectangle` or a `Circle`. Such a schema can
be defined as follows:

@@snip [JsonSchemasDocs.scala](/json-schema/json-schema/src/test/scala/endpoints/algebra/JsonSchemasDocs.scala) { #sum-type-schema }

(We have omitted the definition of `circleSchema` for the sake of conciseness)

First, all the alternative record schemas (in this example, `circeSchema` and `rectangleSchema`) must
be `tagged` with a unique name. Then, the `orElse` operation combines the alternative schemas into a
single schema that accepts one of them.

The result of the `tagged` operation is a `Tagged[A]` schema. This subtype of `JsonSchema[A]` models a
schema that accepts one of several alternative schemas. It provides the `orElse` operation and
adds a discriminator field to the schema.

The `orElse` operation turns the `Tagged[Circle]` and `Tagged[Rectangle]` values into
a `Tagged[Either[Circle, Rectangle]]`, which is then, in this example, transformed into a
`Tagged[Shape]` by using `xmap`.

By default, the discriminator field used to distinguish between tagged alternatives is named
`type`, but you can use another field name either by overriding the `defaultDiscriminatorName`
method of the algebra, or by calling the `withDiscriminator` operation and specifying the
field name to use.

Instead of using `orElse` you can also make use of the `orElseMerge` operation. This is similar to
`orElse`, but requires alternatives to share a parent. In the above example, this requirement is met since both
`Circle` and `Rectangle` extend `Shape`. The `orElseMerge` operation turns the `Tagged[Circle]` and
`Tagged[Rectangle]` values into a `Tagged[Shape]` without any mapping. Note, however, that `orElseMerge`
uses `ClassTag` under the hood, and thus requires both alternatives to have distinct types after erasure.
Our example is valid because `Rectangle` and `Shape` are distinct classes, but consider a type
`Resource[A]`: then the types `Resource[Rectangle]` and `Resource[Circle]` have the same erased type
(`Resource[_]`), making them indistinguishable by the `orElseMerge` operation. See also the documentation
of [`isInstanceOf`](https://www.scala-lang.org/api/current/scala/Any.html#isInstanceOf[T0]:Boolean).

### Refining schemas

The examples above show how to use `xmap` to transform a `JsonSchema[A]` into a `JsonSchema[B]`. In
case the transformation function from `A` to `B` can fail (for example, if it applies additional
validation), you can use `xmapPartial` instead of `xmap`:

@@snip [JsonSchemasFixtures.scala](/json-schema/json-schema/src/test/scala/endpoints/algebra/JsonSchemasFixtures.scala) { #refined }

In this example, we check that the decoded integer is even. If it is not, we return an error message.

### Enumerations

There are different ways to represent enumerations in Scala:

- `scala.util.Enumeration`
- Sealed trait with case objects
- Third-party libraries, e.g. Enumeratum

For example, an enumeration with three possible values can be defined as a sealed trait with three case objects:

@@snip [JsonSchemasDocs.scala](/json-schema/json-schema/src/test/scala/endpoints/algebra/JsonSchemasDocs.scala) { #enum-status }

The method `stringEnumeration` in the `JsonSchemas` algebra supports mapping the enum values to JSON strings.
It has two parameters: the possible values, and a function to encode an enum value as a string.

@@snip [JsonSchemasDocs.scala](/json-schema/json-schema/src/test/scala/endpoints/algebra/JsonSchemasDocs.scala) { #enum-status-schema }

The resulting `JsonSchema[Status]` allows defining JSON members with string values that are mapped to
our case objects.

It will work similarly for other representations of enumerated values.
Most of them provide `values` which can conveniently be passed into `stringEnumeration`.
However, it is still possible to explicitly pass a certain subset of allowed values.

### Tuples

JSON schemas for tuples from 2 to 22 elements are provided out of the box. For instance, if
there are implicit `JsonSchema` instances for types `A`, `B`, and `C`, then you can summon
a `JsonSchema[(A, B, C)]`. Tuples are modeled in JSON with arrays, as recommended in the
[JSON Schema documentation](https://json-schema.org/understanding-json-schema/reference/array.html#tuple-validation).

Here is an example of JSON schema for a GeoJSON `Point`, where GPS coordinates are modeled with a pair (longitude, latitude):

@@snip [JsonSchemasDocs.scala](/json-schema/json-schema/src/test/scala/endpoints/algebra/JsonSchemasDocs.scala) { #tuple }

### Recursive types

You can reference a currently being defined schema without causing a `StackOverflow` error
by wrapping it in the `lazyRecord` or `lazyTagged` constructor:

@@snip [JsonSchemasDocs.scala](/json-schema/json-schema/src/test/scala/endpoints/algebra/JsonSchemasDocs.scala) { #recursive }

### Alternatives between schemas

You can define a schema as an alternative between other schemas with the operation
`orFallbackTo`:

@@snip [JsonSchemasFixtures.scala](/json-schema/json-schema/src/test/scala/endpoints/algebra/JsonSchemasFixtures.scala) { #one-of }

@@@warning
Because decoders derived from schemas defined with the operation `orFallbackTo` literally
“fallback” from one alternative to another, it makes it impossible to report good decoding
failure messages. You should generally prefer using `orElse` on “tagged” schemas.
@@@

### Schemas documentation

Schema descriptions can include documentation information which is used by documentation
interpreters such as the @ref[OpenAPI](../interpreters/openapi.md) interpreter. We have already
seen in the first section that object fields could be documented with a description.
This section shows other features related to schemas documentation.

You can include a description and an example of value for a schema (see the
[Swagger “Adding Examples” documentation](https://swagger.io/docs/specification/adding-examples/)),
with the operations `withDescription` and `withExample`, respectively:

@@snip [JsonSchemasDocs.scala](/json-schema/json-schema/src/test/scala/endpoints/algebra/JsonSchemasDocs.scala) { #with-example }

Applying the OpenAPI interpreter to this schema definition produces the
following JSON document:

~~~ javascript
{
  "type": "object",
  "properties": {
    "width": {
      "type": "number",
      "format":"double",
      "description": "Rectangle width"
    },
    "height":{
      "type": "number",
      "format": "double"
    }
  },
  "required": ["width","height"],
  "description": "A rectangle shape",
  "example": { "width": 10, "height": 20 }
}
~~~

The encoding of sealed traits in OpenAPI can be configured by overriding the `coproductEncoding`
method in the OpenAPI interpreter. By default, the OpenAPI interpreter will encode variants of
sealed traits in the same way that they would be encoded if they were standalone records. However,
it is sometimes useful to include in each variants' schema a reference to the base type schema.
The @scaladoc[API documentation](endpoints.openapi.JsonSchemas) has more details.

You can give names to schemas. These names are used by the OpenAPI interpreter to group
the schema definitions at one place, and then reference each schema by its name (see the
[Swagger “Components Section” documentation](https://swagger.io/docs/specification/components/)).

Use the `named` method to give a name to a `Record`, a `Tagged`, or an `Enum` schema.

@@@ warning
Note that schema names [must be valid URLs](https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.3.md#relative-references-in-urls).
@@@

## Generic derivation of JSON schemas (based on Shapeless) 

The module presented in this section uses Shapeless to generically derive JSON schemas
for algebraic data type definitions (sealed traits and case classes).

@@@vars
~~~ scala
"org.julienrf" %% "endpoints-json-schema-generic" % "$version$"
~~~
@@@

@scaladoc[API documentation](endpoints.generic.JsonSchemas)

### JSON schemas derivation

With this module, defining the JSON schema of the `Shape` data type is
reduced to the following:

@@snip [JsonSchemasDocs.scala](/json-schema/json-schema-generic/src/test/scala/endpoints/generic/JsonSchemasDocs.scala) { #generic-schema }

The `genericJsonSchema` operation builds a JSON schema for the given
type. The rules for deriving the schema are the following:

- the schema of a case class is a JSON object,
- the schema of a sealed trait is the alternative of its leaf case
  class schemas, discriminated by the case class names,
- each case class field has a corresponding required JSON object property of
  the same name and type (for instance, the generic schema for the `Rectangle`
  type has a `width` required property of type `integer`),
- each case class field of type `Option[A]` for some type `A` has a corresponding
  optional JSON object property of the same name and type,
- descriptions can be set for case class fields, case classes, or sealed traits
  by annotating these things with the `@docs` annotation,
- for sealed traits, the discriminator field name can be defined by the `@discriminator`
  annotation, otherwise the `defaultDiscriminatorName` value is used,
- the schema is named by the `@name` annotation, if present, or by invoking the
  `classTagToSchemaName` operation with the `ClassTag` of the type for which the schema
  is derived. If you wish to avoid naming the schema, use the `@unnamed` annotation
  (unnamed schemas get inlined in their OpenAPI documentation).
- the schema title is set with the `@title` annotation, if present

Here is an example that illustrates how to configure the generic schema derivation process:

@@snip [JsonSchemasDocs.scala](/json-schema/json-schema-generic/src/test/scala/endpoints/generic/JsonSchemasDocs.scala) { #documented-generic-schema }

In case you need to transform further a generically derived schema, you might want to use the
`genericRecord` or `genericTagged` operations instead of `genericJsonSchema`. These operations
have a more specific return type than `genericJsonSchema`: `genericRecord` returns a `Record`,
and `genericTagged` returns a `Tagged`.

### JSON schemas transformation

The module also takes advantage shapeless to provide a more convenient `as` operation for
transforming JSON schema definitions, instead of `xmap`:

@@snip [JsonSchemasDocs.scala](/json-schema/json-schema-generic/src/test/scala/endpoints/generic/JsonSchemasDocs.scala) { #explicit-schema }

## Generic derivation of JSON schemas (based on macros)

An alternative to the module presented in the preceding section is provided
as a third-party module:
[endpoints-json-schemas-macros](https://github.com/scalalandio/endpoints-json-schemas-macros).

Please see the README of that project for more information on how to use it
and its differences with the module provided by *endpoints*.
