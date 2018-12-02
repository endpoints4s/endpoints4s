# OpenAPI

~~~ scala expandVars=true
"org.julienrf" %% "endpoints-openapi" % "{{version}}"
~~~

[API documentation](api:endpoints.openapi.package)

## Endpoints

The `Endpoints` interpreter provides an `openApi` method
that takes as parameter a sequence of endpoints for which
to generate an OpenAPI document.

Given the following endpoint definition:

~~~ scala src=../../../../../algebras/algebra/src/test/scala/endpoints/algebra/EndpointsDocs.scala#endpoint-definition
~~~

It can be documented as follows:

~~~ scala src=../../../../../openapi/openapi/src/test/scala/endpoints/openapi/EndpointsDocs.scala#documentation
~~~

The value returned by the `openApi` method has type `endpoints.openapi.models.OpenApi`,
which is an abstract model for OpenAPI documents. You can generate a JSON
representation of it, which you can then publish by your server, by
serializing it into JSON. For instance, using circe:

~~~ scala src=../../../../../openapi/openapi/src/test/scala/endpoints/openapi/EndpointsDocs.scala#documentation-asjson
~~~

## JSON entities

To properly document the underlying JSON schema of your JSON entities,
you have to define these schemas by using the
[JsonSchemaEntities](/algebras/json-entities.md#jsonschemaentities)
algebra (and its corresponding interpreter).
