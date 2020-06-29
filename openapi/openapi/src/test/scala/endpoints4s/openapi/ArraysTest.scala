package endpoints4s.openapi

import endpoints4s.openapi.model.{Info, OpenApi}
import endpoints4s.{algebra, openapi}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ArraysTest extends AnyWordSpec with Matchers {

  trait Arrays extends algebra.Endpoints with algebra.JsonEntitiesFromSchemas {

    val arrays: Endpoint[List[String], (Boolean, Int, String)] = endpoint(
      post(path / "foo", jsonRequest[List[String]]),
      ok(jsonResponse[(Boolean, Int, String)])
    )

  }

  object ArraysDocumentation
      extends Arrays
      with openapi.Endpoints
      with openapi.JsonEntitiesFromSchemas {

    val api: OpenApi = openApi(
      Info(
        title = "Example of API using homogeneous and heterogeneous arrays",
        version = "0.0.0"
      )
    )(arrays)

  }

  "Arrays" should {

    val expectedSchema =
      """{
        |  "openapi" : "3.0.0",
        |  "info" : {
        |    "title" : "Example of API using homogeneous and heterogeneous arrays",
        |    "version" : "0.0.0"
        |  },
        |  "paths" : {
        |    "/foo" : {
        |      "post" : {
        |        "requestBody" : {
        |          "content" : {
        |            "application/json" : {
        |              "schema" : {
        |                "type" : "array",
        |                "items" : {
        |                  "type" : "string"
        |                }
        |              }
        |            }
        |          }
        |        },
        |        "responses" : {
        |          "400" : {
        |            "description" : "Client error",
        |            "content" : {
        |              "application/json" : {
        |                "schema" : {
        |                  "$ref" : "#/components/schemas/endpoints.Errors"
        |                }
        |              }
        |            }
        |          },
        |          "500" : {
        |            "description" : "Server error",
        |            "content" : {
        |              "application/json" : {
        |                "schema" : {
        |                  "$ref" : "#/components/schemas/endpoints.Errors"
        |                }
        |              }
        |            }
        |          },
        |          "200" : {
        |            "description" : "",
        |            "content" : {
        |              "application/json" : {
        |                "schema" : {
        |                  "type" : "array",
        |                  "items" : [
        |                    {
        |                      "type" : "boolean"
        |                    },
        |                    {
        |                      "type" : "integer",
        |                      "format" : "int32"
        |                    },
        |                    {
        |                      "type" : "string"
        |                    }
        |                  ]
        |                }
        |              }
        |            }
        |          }
        |        }
        |      }
        |    }
        |  },
        |  "components" : {
        |    "schemas" : {
        |      "endpoints.Errors" : {
        |        "type" : "array",
        |        "items" : {
        |          "type" : "string"
        |        }
        |      }
        |    },
        |    "securitySchemes" : {
        |
        |    }
        |  }
        |}""".stripMargin

    "be documented" in {
      ujson.read(OpenApi.stringEncoder.encode(ArraysDocumentation.api)) shouldBe ujson
        .read(expectedSchema)
    }

  }

}
