# JSON schemas

## `JsonSchemas`

This algebra provides vocabulary to define JSON schemas of data types.

~~~ scala expandVars=true
"org.julienrf" %% "endpoints-algebra-json-schema" % "{{version}}"
~~~

[API documentation](api:endpoints.algebra.JsonSchemas)

> {.note}
> This module is dependency-free, it can be used independently of *endpoints*
> to define JSON schemas and interpret them as actual encoder, decoders or
> documentation.

The algebra introduces the concept of `JsonSchema[A]`: a JSON schema for a type `A`.

The trait provides some predefined JSON schemas (for `String`, `Int`, `Boolean`, etc.)
and ways to combine them together to build more complex schemas.

For instance, given the following `Rectangle` data type:

~~~ scala src=../../../../../json-schema/json-schema/src/test/scala/endpoints/algebra/JsonSchemasDocs.scala#record-type
~~~

We can represent instances of `Rectangle` in JSON with a JSON object having properties corresponding
to the case class fields. A JSON schema for such objects would be defined as follows:

~~~ scala src=../../../../../json-schema/json-schema/src/test/scala/endpoints/algebra/JsonSchemasDocs.scala#record-schema
~~~

The `field` constructor defines a JSON object schema with one field of the given
type and name (and an optional text documentation). A similar constructor, `optField`,
defines an optional field in a JSON object.

In the above example, we define two JSON object schemas (one for the `width` field,
of type `Record[Double]`, and one for the `height` field, of type `Record[Double]`),
and then we combine them into a single JSON object schema by using the `zip` operation. Finally, we call the `invmap` operation
to turn the `Record[(Double, Double)]` value returned by the `zip` operation into
a `Record[Rectangle]`.

### Sum types

It is also possible to define schemas for sum types. Consider the following type definition,
defining a `Shape`, which can be either a `Circle` or a `Rectangle`:

~~~ scala src=../../../../../json-schema/json-schema/src/test/scala/endpoints/algebra/JsonSchemasDocs.scala#sum-type
~~~

A possible JSON schema for this data type consists in using a JSON object with a discriminator
field indicating whether the `Shape` is a `Rectangle` or a `Circle`. Such a schema can
be defined as follows:

~~~ scala src=../../../../../json-schema/json-schema/src/test/scala/endpoints/algebra/JsonSchemasDocs.scala#sum-type-schema
~~~

(We have omitted the definition of `circleSchema` for the sake of conciseness)

The `orElse` operation turns the `Record[Circle]` and `Record[Rectangle]` values into
a `Record[Either[Circle, Rectangle]]`, which is then transformed into a `Record[Shape]` by
using `invmap`.

By default, the discriminator field is named `type`, but you can use another field name if
you want to.

### Enumerations

There are different ways to represent enumerations in Scala:

- `scala.util.Enumeration`
- Sealed trait with case objects
- Third-party libraries, e.g. Enumeratum

For example, an enumeration with three possible values can be defined as a sealed trait with three case objects:

~~~ scala src=../../../../../json-schema/json-schema/src/test/scala/endpoints/algebra/JsonSchemasDocs.scala#enum-status
~~~

The method `enumeration` in the `JsonSchemas` algebra supports mapping the enum values to JSON strings.
It has two parameters: the possible values, and a function to encode an enum value as a string.

~~~ scala src=../../../../../json-schema/json-schema/src/test/scala/endpoints/algebra/JsonSchemasDocs.scala#enum-status-schema
~~~

The resulting `JsonSchema[Status]` allows defining JSON members with string values that are mapped to our case objects.

It will work similarly for other representations of enumerated values.
Most of them provide `values` which can conveniently be passed into `enumeration`.
However, it is still possible to explicitly pass a certain subset of allowed values.

## Generic derivation of JSON schemas

The module presented in this section uses Shapeless to generically derive JSON schemas
for algebraic data types.

~~~ scala expandVars=true
"org.julienrf" %% "endpoints-json-schema-generic" % "{{version}}"
~~~

[API documentation](api:endpoints.generic.JsonSchemas)

With this module, defining the JSON schema of the `Shape` data type is
reduced to the following:

~~~ scala src=../../../../../json-schema/json-schema-generic/src/test/scala/endpoints/generic/JsonSchemasDocs.scala#generic-schema
~~~

The `genericJsonSchema` operation builds a JSON schema for the given
type, mapping each case class field to a JSON object field of the same name,
and each leaf of a sealed trait to a tagged JSON object whose type discriminator
uses the case class name.

The module also takes advantage shapeless to define more convenient
operations for combining JSON schema definitions: the `zip` operation
is replaced by a `:*:` operator, and `invmap` is replaced by `as`:

~~~ scala src=../../../../../json-schema/json-schema-generic/src/test/scala/endpoints/generic/JsonSchemasDocs.scala#explicit-schema
~~~
