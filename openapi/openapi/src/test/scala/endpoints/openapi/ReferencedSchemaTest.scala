package endpoints.openapi

import endpoints.openapi.model._
import endpoints.{algebra, generic, openapi}
import org.scalatest.{Matchers, WordSpec}

class ReferencedSchemaTest extends WordSpec with Matchers {

  sealed trait Storage

  object Storage {
    case class Library(room: String, shelf: Int) extends Storage
    case class Online(link: String) extends Storage
  }

  case class Author(name: String)

  case class Book(id: Int, title: String, author: Author, isbnCodes: List[String], storage: Storage)

  object Fixtures extends Fixtures with openapi.Endpoints with openapi.JsonSchemaEntities with openapi.BasicAuthentication {

    def openApiDocument: OpenApi = openApi(
      Info(title = "TestFixturesOpenApi", version = "0.0.0")
    )(Fixtures.listBooks, Fixtures.postBook)
  }

  trait Fixtures extends algebra.Endpoints with algebra.JsonSchemaEntities with generic.JsonSchemas with algebra.BasicAuthentication with algebra.JsonSchemasTest {

    implicit private val schemaStorage: JsonSchema[Storage] =
      withDiscriminator(genericTagged[Storage], "storageType")

    implicit val schemaAuthor: JsonSchema[Author] = (
      field[String]("name", documentation = Some("Author name")).xmap[Author](Author)(_.name)
    )

    implicit private val schemaBook: JsonSchema[Book] = genericJsonSchema[Book]

    val listBooks = endpoint(get(path / "books"), ok(jsonResponse[List[Book]], Some("Books list")), docs = EndpointDocs(tags = List("Books")))

    val postBook =
      authenticatedEndpoint(
        Post, path / "books", ok(jsonResponse(Enum.colorSchema)), jsonRequest[Book], requestDocs = Some("Books list"), endpointDocs = EndpointDocs(tags = List("Books")))
  }

  "OpenApi" should {

    val expectedSchema =
      """{
        |  "openapi" : "3.0.0",
        |  "info" : {
        |    "title" : "TestFixturesOpenApi",
        |    "version" : "0.0.0"
        |  },
        |  "paths" : {
        |    "/books" : {
        |      "get" : {
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
        |            "description" : "Books list",
        |            "content" : {
        |              "application/json" : {
        |                "schema" : {
        |                  "type" : "array",
        |                  "items" : {
        |                    "$ref" : "#/components/schemas/endpoints.openapi.ReferencedSchemaTest.Book"
        |                  }
        |                }
        |              }
        |            }
        |          }
        |        },
        |        "tags" : [
        |          "Books"
        |        ]
        |      },
        |      "post" : {
        |        "requestBody" : {
        |          "content" : {
        |            "application/json" : {
        |              "schema" : {
        |                "$ref" : "#/components/schemas/endpoints.openapi.ReferencedSchemaTest.Book"
        |              }
        |            }
        |          },
        |          "description" : "Books list"
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
        |            "content": {
        |              "application/json": {
        |                "schema": { "$ref": "#/components/schemas/Color" }
        |              }
        |            }
        |          },
        |          "403" : {
        |            "description" : ""
        |          }
        |        },
        |        "tags" : [
        |          "Books"
        |        ],
        |        "security" : [
        |          {
        |            "HttpBasic" : [
        |            ]
        |          }
        |        ]
        |      }
        |    }
        |  },
        |  "components" : {
        |    "schemas" : {
        |      "Color" : {
        |        "type" : "string",
        |        "enum" : ["Red", "Blue"]
        |      },
        |      "endpoints.openapi.ReferencedSchemaTest.Storage.Online" : {
        |        "allOf" : [
        |          {
        |            "$ref" : "#/components/schemas/endpoints.openapi.ReferencedSchemaTest.Storage"
        |          },
        |          {
        |            "type" : "object",
        |            "properties" : {
        |              "storageType" : {
        |                "type" : "string",
        |                "enum" : ["Online"]
        |              },
        |              "link" : {
        |                "type" : "string"
        |              }
        |            },
        |            "required" : [
        |              "storageType",
        |              "link"
        |            ]
        |          }
        |        ]
        |      },
        |      "endpoints.openapi.ReferencedSchemaTest.Book" : {
        |        "type" : "object",
        |        "properties" : {
        |          "isbnCodes" : {
        |            "type" : "array",
        |            "items" : {
        |              "type" : "string"
        |            }
        |          },
        |          "author" : {
        |            "type" : "object",
        |            "properties" : {
        |              "name" : {
        |                "type" : "string",
        |                "description" : "Author name"
        |              }
        |            },
        |            "required" : [
        |              "name"
        |            ]
        |          },
        |          "id" : {
        |            "type" : "integer",
        |            "format" : "int32"
        |          },
        |          "storage" : {
        |            "$ref" : "#/components/schemas/endpoints.openapi.ReferencedSchemaTest.Storage"
        |          },
        |          "title" : {
        |            "type" : "string"
        |          }
        |        },
        |        "required" : [
        |          "isbnCodes",
        |          "author",
        |          "id",
        |          "storage",
        |          "title"
        |        ]
        |      },
        |      "endpoints.openapi.ReferencedSchemaTest.Storage" : {
        |        "oneOf" : [
        |          {
        |            "$ref" : "#/components/schemas/endpoints.openapi.ReferencedSchemaTest.Storage.Library"
        |          },
        |          {
        |            "$ref" : "#/components/schemas/endpoints.openapi.ReferencedSchemaTest.Storage.Online"
        |          }
        |        ],
        |        "discriminator" : {
        |          "propertyName" : "storageType",
        |          "mapping" : {
        |            "Library" : "#/components/schemas/endpoints.openapi.ReferencedSchemaTest.Storage.Library",
        |            "Online" : "#/components/schemas/endpoints.openapi.ReferencedSchemaTest.Storage.Online"
        |          }
        |        }
        |      },
        |      "endpoints.Errors" : {
        |        "type" : "array",
        |        "items" : {
        |          "type" : "string"
        |        }
        |      },
        |      "endpoints.openapi.ReferencedSchemaTest.Storage.Library" : {
        |        "allOf" : [
        |          {
        |            "$ref" : "#/components/schemas/endpoints.openapi.ReferencedSchemaTest.Storage"
        |          },
        |          {
        |            "type" : "object",
        |            "properties" : {
        |              "storageType" : {
        |                "type" : "string",
        |                "enum" : ["Library"]
        |              },
        |              "room" : {
        |                "type" : "string"
        |              },
        |              "shelf" : {
        |                "type" : "integer",
        |                "format" : "int32"
        |              }
        |            },
        |            "required" : [
        |              "storageType",
        |              "room",
        |              "shelf"
        |            ]
        |          }
        |        ]
        |      }
        |    },
        |    "securitySchemes" : {
        |      "HttpBasic" : {
        |        "type" : "http",
        |        "description" : "Http Basic Authentication",
        |        "scheme" : "basic"
        |      }
        |    }
        |  }
        |}""".stripMargin

    "produce referenced schema with circe" in {

      object OpenApiEncoder extends openapi.model.OpenApiSchemas with endpoints.circe.JsonSchemas
      import OpenApiEncoder.JsonSchema._
      import io.circe.syntax._
      import io.circe.parser.parse

      Fixtures.openApiDocument.asJson shouldBe parse(expectedSchema).right.get
    }

    "produce referenced schema with playjson" in {

      object OpenApiEncoder extends openapi.model.OpenApiSchemas with endpoints.playjson.JsonSchemas
      import OpenApiEncoder.JsonSchema._
      import play.api.libs.json.Json

      Json.toJson(Fixtures.openApiDocument) shouldBe Json.parse(expectedSchema)
    }
  }
}
