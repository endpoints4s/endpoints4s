package endpoints.openapi

import endpoints.openapi.model.{Info, OpenApi}
import endpoints.{algebra, openapi}
import org.scalatest.{Matchers, WordSpec}

class WebhooksTest extends WordSpec with Matchers {

  case class Message(value: String)

  trait Webhooks extends algebra.Endpoints with algebra.JsonSchemaEntities {

    implicit lazy val messageSchema: JsonSchema[Message] =
      named(field[String]("message"), "webhook.Message")
        .xmap(Message(_))(_.value)

    val subscribe: Endpoint[String, Unit] = endpoint(
      post(path / "subscribe" /? qs[String]("callbackURL"), emptyRequest),
      ok(emptyResponse),
      docs = EndpointDocs(
        callbacks = Map(
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

  object WebhooksDocumentation extends Webhooks with openapi.Endpoints with openapi.JsonSchemaEntities {

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

    "be documented with circe" in {
      object OpenApiEncoder extends openapi.model.OpenApiSchemas with endpoints.circe.JsonSchemas
      import OpenApiEncoder.JsonSchema._
      import io.circe.syntax._
      import io.circe.parser.parse

      WebhooksDocumentation.api.asJson shouldBe parse(expectedSchema).right.get
    }

    "be documented with playjson" in {
      object OpenApiEncoder extends openapi.model.OpenApiSchemas with endpoints.playjson.JsonSchemas
      import OpenApiEncoder.JsonSchema._
      import play.api.libs.json.Json

      Json.toJson(WebhooksDocumentation.api) shouldBe Json.parse(expectedSchema)
    }

  }

}
