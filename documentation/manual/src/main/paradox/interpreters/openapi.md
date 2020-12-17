# OpenAPI

@coordinates[openapi]

@scaladoc[API documentation](endpoints4s.openapi.index)

This family of interpreters produces static documentation for endpoint definitions,
in the form of an [OpenAPI document](https://www.openapis.org/).

## Endpoints

The `Endpoints` interpreter provides an `openApi` method
that takes as parameter a sequence of endpoints for which
to generate an OpenAPI document.

Given the following endpoint definition:

@@snip [EndpointsDocs.scala](/algebras/algebra/src/test/scala/endpoints4s/algebra/EndpointsDocs.scala) { #documented-endpoint-definition }

It can be documented as follows:

@@snip [EndpointsDocs.scala](/openapi/openapi/src/test/scala/endpoints4s/openapi/EndpointsDocs.scala) { #documentation }

The value returned by the `openApi` method has type `endpoints4s.openapi.models.OpenApi`,
which is an abstract model for OpenAPI documents. You can encode it into JSON by using the
`OpenApi.stringEncoder` encoder. 

@@snip [EndpointsDocs.scala](/openapi/openapi/src/test/scala/endpoints4s/openapi/EndpointsDocs.scala) { #documentation-asjson }

In case the endpoint that serves the documentation is itself defined using _endpoints_,
you can use the `JsonEntitiesFromEncoderAndDecoder` interpreter to define an endpoint
returning the `OpenApi` document as a JSON entity. Here is an example using Akka
HTTP:

@@snip [Main.scala](/documentation/examples/quickstart/server/src/main/scala/quickstart/Main.scala) { #serving-documentation }

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

To properly document the underlying JSON schema of your @ref[JSON entities](../algebras/json-entities.md),
you have to define these schemas by using the
@ref[JsonEntitiesFromSchemas](../algebras/json-entities.md#jsonentitiesfromschemas)
algebra (and its corresponding interpreter).
