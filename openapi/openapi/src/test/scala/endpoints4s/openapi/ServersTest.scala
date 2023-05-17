package endpoints4s.openapi

import endpoints4s.openapi.model.{Info, OpenApi, Server, ServerVariable}
import endpoints4s.{algebra, openapi}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ServersTest extends AnyWordSpec with Matchers {

  trait MyEndpoints extends algebra.Endpoints {

    val foo: Endpoint[String, Unit] = endpoint(
      get(path / "foo" /? qs[String]("q")),
      ok(emptyResponse)
    )

  }

  object MyEndpointsDocumentation
      extends MyEndpoints
      with openapi.Endpoints {

    val api: OpenApi =
      openApi(Info(title = "My API", version = "0.0.0"))(foo)
        .withServers(
          Seq(
            Server("https://my-server.com:{port}")
              .withDescription(Some("The production server"))
              .withVariables(
                Map(
                  "port" -> ServerVariable("443")
                    .withEnum(Some(Seq("443", "8443")))
                    .withDescription(Some("Port number"))
                )
              )
          )
        )

  }

  "Servers" should {

    val expectedSchema =
      """{
        |  "openapi": "3.0.0",
        |  "info": {
        |    "title": "My API",
        |    "version": "0.0.0"
        |  },
        |  "paths": {
        |    "/foo": {
        |      "get": {
        |        "responses": {
        |          "200": {
        |            "description": ""
        |          },
        |          "400": {
        |            "description": "Client error",
        |            "content": {
        |              "application/json": {
        |                "schema": {
        |                  "$ref": "#/components/schemas/endpoints.Errors"
        |                }
        |              }
        |            }
        |          },
        |          "500": {
        |            "description": "Server error",
        |            "content": {
        |              "application/json": {
        |                "schema": {
        |                  "$ref": "#/components/schemas/endpoints.Errors"
        |                }
        |              }
        |            }
        |          }
        |        },
        |        "parameters": [
        |          {
        |            "name": "q",
        |            "in": "query",
        |            "schema": {
        |              "type": "string"
        |            },
        |            "required": true
        |          }
        |        ]
        |      }
        |    }
        |  },
        |  "servers": [
        |    {
        |      "url": "https://my-server.com:{port}",
        |      "description": "The production server",
        |      "variables": {
        |        "port": {
        |          "default": "443",
        |          "description": "Port number",
        |          "enum":["443","8443"]
        |        }
        |      }
        |    }
        |  ],
        |  "components": {
        |    "schemas": {
        |      "endpoints.Errors": {
        |        "type": "array",
        |        "items": {
        |          "type": "string"
        |        }
        |      }
        |    },
        |    "securitySchemes": {}
        |  }
        |}""".stripMargin

    "be documented" in {
      ujson.read(OpenApi.stringEncoder.encode(MyEndpointsDocumentation.api)) shouldBe ujson
        .read(expectedSchema)
    }

  }

}
