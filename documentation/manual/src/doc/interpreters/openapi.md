# OpenAPI

~~~ scala expandVars=true
"org.julienrf" %% "endpoints-openapi" % "{{version}}"
~~~

[API documentation](unchecked:/api/endpoints/openapi/index.html)

## Endpoints

The `Endpoints` interpreter provides an `openApi` method
that takes as parameter a sequence of endpoints for which
to generate an OpenAPI document.

Given the following endpoint definition:

~~~ scala src=../../../../../algebras/algebra/src/test/scala/endpoints/algebra/EndpointsDocs.scala#documented-endpoint-definition
~~~

It can be documented as follows:

~~~ scala src=../../../../../openapi/openapi/src/test/scala/endpoints/openapi/EndpointsDocs.scala#documentation
~~~

The value returned by the `openApi` method has type `endpoints.openapi.models.OpenApi`,
which is an abstract model for OpenAPI documents. You can generate a JSON
representation of it, which you can then publish to your server. For instance, using circe:

~~~ scala src=../../../../../openapi/openapi/src/test/scala/endpoints/openapi/EndpointsDocs.scala#documentation-asjson
~~~

The `OpenApiSchemas` trait (mixed into the top-level object) provides a
[`JsonSchema`](/algebras/json-schemas.md) instance for the `OpenApi` model, which
is used by the circe `JsonSchemas` interpreter to turn the `api` value into a `Json`
document.

Finally, the `apiJson` value contains the following JSON document:

~~~ json
{
  "openapi" : "3.0.0",
  "info" : {
    "title" : "API to get some resource",
    "version" : "1.0"
  },
  "paths" : {
    "/some-resource/{id}" : {
      "get" : {
        "parameters" : [
          {
            "name" : "id",
            "in" : "path",
            "schema" : {
              "type" : "integer"
            },
            "required" : true
          }
        ],
        "responses" : {
          "200" : {
            "description" : "The content of the resource",
            "content" : {
              "text/plain" : {
                "schema" : {
                  "type" : "string"
                }
              }
            }
          }
        }
      }
    }
  }
}
~~~

## JSON entities

To properly document the underlying JSON schema of your [JSON entities](/algebras/json-entities.md),
you have to define these schemas by using the
[JsonSchemaEntities](/algebras/json-entities.md#jsonschemaentities)
algebra (and its corresponding interpreter).
