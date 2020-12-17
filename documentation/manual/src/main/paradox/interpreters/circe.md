# Circe

Builds [Circe](http://circe.github.io/circe/) codecs out of JSON schema definitions.

@coordinates[json-schema-circe]

@scaladoc[API documentation](endpoints4s.circe.JsonSchemas)

The `JsonSchemas` interpreter fixes the `JsonSchema[A]` to a type
that provides both an `io.circe.Encoder[A]` and an `io.circe.Decoder[A]`.

Given the following type definition:

@@snip [JsonSchemasDocs.scala](/json-schema/json-schema/src/test/scala/endpoints4s/algebra/JsonSchemasDocs.scala) { #sum-type }

Assuming that there is an implicit `JsonSchema[Shape]` definition,
we can encode a `Shape` into JSON and decode it using the usual
circe operations:

@@snip [JsonSchemasDocs.scala](/json-schema/json-schema-circe/src/test/scala/endpoints4s/circe/JsonSchemasDocs.scala) { #codec }
