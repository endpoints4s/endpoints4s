package endpoints4s.openapi

import java.util.UUID

import endpoints4s.algebra.{ExternalDocumentationObject, Tag}
import endpoints4s.generic.discriminator
import endpoints4s.openapi.model._
import endpoints4s.{algebra, generic, openapi}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ReferencedSchemaTest extends AnyWordSpec with Matchers {

  @discriminator("storageType")
  sealed trait Storage

  object Storage {
    case class Library(room: String, shelf: Int) extends Storage
    case class Online(link: String) extends Storage
  }

  case class Author(name: String)

  case class Book(
      id: UUID,
      title: String,
      author: Author,
      isbnCodes: List[String],
      storage: Storage
  )

  object Fixtures
      extends Fixtures
      with openapi.Endpoints
      with openapi.JsonEntitiesFromSchemas
      with openapi.BasicAuthentication {

    def openApiDocument: OpenApi =
      openApi(
        Info(title = "TestFixturesOpenApi", version = "0.0.0")
          .withDescription(Some("This is a top level description."))
      )(Fixtures.listBooks, Fixtures.postBook)
  }

  trait Fixtures
      extends algebra.Endpoints
      with algebra.JsonEntitiesFromSchemas
      with generic.JsonSchemas
      with algebra.BasicAuthentication
      with algebra.JsonSchemasFixtures {

    implicit private val schemaStorage: JsonSchema[Storage] =
      genericTagged[Storage]

    implicit val schemaAuthor: JsonSchema[Author] = (
      field[String]("name", documentation = Some("Author name"))
        .xmap[Author](Author)(_.name)
    )

    implicit private val schemaBook: JsonSchema[Book] = genericJsonSchema[Book]

    val bookTag = Tag("Books")
      .withDescription(Some("A book is something you can read."))
      .withExternalDocs(
        Some(
          ExternalDocumentationObject("moreinfo@books.nl")
            .withDescription(Some("The official website about books."))
        )
      )

    val listBooks = endpoint(
      get(path / "books"),
      ok(jsonResponse[List[Book]], Some("Books list")),
      docs = EndpointDocs().withTags(List(bookTag))
    )

    val postBook =
      authenticatedEndpoint(
        Post,
        path / "books",
        ok(jsonResponse(Enum.colorSchema)),
        jsonRequest[Book],
        requestDocs = Some("Books list"),
        endpointDocs = EndpointDocs().withTags(List(bookTag, Tag("Another tag")))
      )
  }

  "OpenApi" should {

    val expectedSchema =
      """{
        |  "openapi" : "3.0.0",
        |  "info" : {
        |    "title" : "TestFixturesOpenApi",
        |    "version" : "0.0.0",
        |    "description": "This is a top level description."
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
        |                    "$ref" : "#/components/schemas/endpoints4s.openapi.ReferencedSchemaTest.Book"
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
        |                "$ref" : "#/components/schemas/endpoints4s.openapi.ReferencedSchemaTest.Book"
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
        |          "Books",
        |          "Another tag"
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
        |  "tags": [
        |    {
        |      "name":"Books",
        |      "description":"A book is something you can read.",
        |      "externalDocs": {
        |        "url":"moreinfo@books.nl",
        |        "description":"The official website about books."
        |      }
        |    }
        |  ],
        |  "components" : {
        |    "schemas" : {
        |      "Color" : {
        |        "type" : "string",
        |        "enum" : ["Red", "Blue"]
        |      },
        |      "endpoints4s.openapi.ReferencedSchemaTest.Storage.Online" : {
        |        "type" : "object",
        |        "properties" : {
        |          "storageType" : {
        |            "type" : "string",
        |            "enum" : ["Online"],
        |            "example" : "Online"
        |          },
        |          "link" : {
        |            "type" : "string"
        |          }
        |        },
        |        "required" : [
        |          "storageType",
        |          "link"
        |        ]
        |      },
        |      "endpoints4s.openapi.ReferencedSchemaTest.Book" : {
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
        |            "type" : "string",
        |            "format" : "uuid",
        |            "description": "Universally unique identifier (RFC 4122)",
        |            "example": "5f27b818-027a-4008-b410-de01e1dd3a93"
        |          },
        |          "storage" : {
        |            "$ref" : "#/components/schemas/endpoints4s.openapi.ReferencedSchemaTest.Storage"
        |          },
        |          "title" : {
        |            "type" : "string"
        |          }
        |        },
        |        "required" : [
        |          "id",
        |          "title",
        |          "author",
        |          "isbnCodes",
        |          "storage"
        |        ]
        |      },
        |      "endpoints4s.openapi.ReferencedSchemaTest.Storage" : {
        |        "oneOf" : [
        |          {
        |            "$ref" : "#/components/schemas/endpoints4s.openapi.ReferencedSchemaTest.Storage.Library"
        |          },
        |          {
        |            "$ref" : "#/components/schemas/endpoints4s.openapi.ReferencedSchemaTest.Storage.Online"
        |          }
        |        ],
        |        "discriminator" : {
        |          "propertyName" : "storageType",
        |          "mapping" : {
        |            "Library" : "#/components/schemas/endpoints4s.openapi.ReferencedSchemaTest.Storage.Library",
        |            "Online" : "#/components/schemas/endpoints4s.openapi.ReferencedSchemaTest.Storage.Online"
        |          }
        |        }
        |      },
        |      "endpoints.Errors" : {
        |        "type" : "array",
        |        "items" : {
        |          "type" : "string"
        |        }
        |      },
        |      "endpoints4s.openapi.ReferencedSchemaTest.Storage.Library" : {
        |        "type" : "object",
        |        "properties" : {
        |          "storageType" : {
        |            "type" : "string",
        |            "enum" : ["Library"],
        |            "example" : "Library"
        |          },
        |          "room" : {
        |            "type" : "string"
        |          },
        |          "shelf" : {
        |            "type" : "integer",
        |            "format" : "int32"
        |          }
        |        },
        |        "required" : [
        |          "storageType",
        |          "room",
        |          "shelf"
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

    "be documented" in {
      val actual =
        ujson.read(OpenApi.stringEncoder.encode(Fixtures.openApiDocument))
      actual shouldBe ujson.read(expectedSchema)
    }

  }
}
