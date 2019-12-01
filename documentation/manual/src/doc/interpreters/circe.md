# circe

Builds [circe](http://circe.github.io/circe/) codecs out of JSON schema definitions.

~~~ scala expandVars=true
"org.julienrf" %% "endpoints-json-schema-circe" % "{{version}}"
~~~

[API documentation](unchecked:/api/endpoints/circe/JsonSchemas.html)

The `JsonSchemas` interpreter fixes the `JsonSchema[A]` to a type
that provides both an `io.circe.Encoder[A]` and an `io.circe.Decoder[A]`.

Given the following type definition:

~~~ scala src=../../../../../json-schema/json-schema/src/test/scala/endpoints/algebra/JsonSchemasDocs.scala#sum-type
~~~

Assuming that there is an implicit `JsonSchema[Shape]` definition,
we can encode a `Shape` into JSON and decode it using the usual
circe operations:

~~~ scala src=../../../../../json-schema/json-schema-circe/src/test/scala/endpoints/circe/JsonSchemasDocs.scala#codec
~~~
