package endpoints4s.openapi

import endpoints4s.openapi.model.{Info, OpenApi}
import endpoints4s.{algebra, openapi}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class WebhooksTest extends AnyWordSpec with Matchers {

  case class Message(value: String)

  trait Webhooks extends algebra.Endpoints with algebra.JsonEntitiesFromSchemas {

    implicit lazy val messageSchema: JsonSchema[Message] =
      field[String]("message")
        .named("webhook.Message")
        .xmap(Message(_))(_.value)

    val subscribe: Endpoint[String, Unit] = endpoint(
      post(path / "subscribe" /? qs[String]("callbackURL"), emptyRequest),
      ok(emptyResponse),
      docs = EndpointDocs()
        .withCallbacks(
          Map(
            "message" -> Map(
              "{$request.query.callbackURL}" -> CallbackDocs(
                Post,
                jsonRequest[Message],
                ok(emptyResponse)
              )
            )
          )
        )
    )

  }

  object WebhooksDocumentation
      extends Webhooks
      with openapi.Endpoints
      with openapi.JsonEntitiesFromSchemas {

    val api: OpenApi = openApi(
      Info(title = "Example of API using callbacks", version = "0.0.0")
    )(subscribe)

  }

  "Callbacks" should {

    val expectedSchema =
      """{
        |  "openapi" : "3.0.0",
        |  "info" : {
        |    "title" : "Example of API using callbacks",
        |    "version" : "0.0.0"
        |  },
        |  "paths" : {
        |    "/subscribe" : {
        |      "post" : {
        |        "parameters" : [
        |          {
        |            "name" : "callbackURL",
        |            "in" : "query",
        |            "schema" : {
        |              "type" : "string"
        |            },
        |            "required" : true
        |          }
        |        ],
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
        |            "description" : ""
        |          }
        |        },
        |        "callbacks" : {
        |          "message" : {
        |            "{$request.query.callbackURL}" : {
        |              "post" : {
        |                "requestBody" : {
        |                  "content" : {
        |                    "application/json" : {
        |                      "schema" : {
        |                        "$ref" : "#/components/schemas/webhook.Message"
        |                      }
        |                    }
        |                  }
        |                },
        |                "responses" : {
        |                  "200" : {
        |                    "description" : ""
        |                  }
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
        |      },
        |      "webhook.Message" : {
        |        "type" : "object",
        |        "properties" : {
        |          "message" : {
        |            "type" : "string"
        |          }
        |        },
        |        "required" : ["message"]
        |      }
        |    },
        |    "securitySchemes" : {}
        |  }
        |}""".stripMargin

    "be documented" in {
      ujson.read(OpenApi.stringEncoder.encode(WebhooksDocumentation.api)) shouldBe ujson
        .read(expectedSchema)
    }

  }

}
