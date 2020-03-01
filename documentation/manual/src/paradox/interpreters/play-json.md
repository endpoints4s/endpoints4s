# Play JSON

Builds [Play JSON](https://github.com/playframework/play-json) Reads and Writes out of JSON schema definitions.

@@@vars
~~~ scala
"org.julienrf" %% "endpoints-json-schema-playjson" % "$version$"
~~~
@@@

@scaladoc[API documentation](endpoints.playjson.JsonSchemas)

The `JsonSchemas` interpreter fixes the `JsonSchema[A]` type to a type
that provides both a `Reads[A]` and a `Writes[A]`.

Given the following type definition:

@@snip [JsonSchemasDocs.scala](/json-schema/json-schema/src/test/scala/endpoints/algebra/JsonSchemasDocs.scala) { #sum-type }

Assuming that there is an implicit `JsonSchema[Shape]` definition,
we can encode a `Shape` into JSON and decode it using the usual
Play JSON operations:

@@snip [JsonSchemasDocs.scala](/json-schema/json-schema-playjson/src/test/scala/endpoints/playjson/JsonSchemasDocs.scala) { #codec }
