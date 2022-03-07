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

    val bookExample = Book(
      id = UUID.fromString("44c159ed-cb8b-464e-ad02-644c36ed0b3f"),
      title = "Programming in Scala, First Edition",
      author = Author("Martin Odersky, Lex Spoon, and Bill Venners"),
      isbnCodes = Nil,
      storage = Storage.Online("https://www.artima.com/pins1ed/")
    )

    val bookExample2 = Book(
      id = UUID.fromString("2cb38bf2-ecbf-401c-be37-9ec325ae91a0"),
      title = "Essential Scala",
      author = Author("Noel Welsh and Dave Gurnell"),
      isbnCodes = Nil,
      storage = Storage.Online("https://underscore.io/books/essential-scala/")
    )

    implicit private val schemaStorage: JsonSchema[Storage] =
      genericTagged[Storage]

    implicit val schemaAuthor: JsonSchema[Author] = (
      field[String]("name", documentation = Some("Author name"))
        .xmap[Author](Author(_))(_.name)
    )

    implicit private val schemaBook: JsonSchema[Book] = genericJsonSchema[Book]
      .withExample(bookExample)

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
        ok(jsonResponse(ColorEnum.colorSchema.withExample(ColorEnum.Blue))),
        jsonRequest[Book](schemaBook.withExample(bookExample2)),
        requestDocs = Some("Books list"),
        endpointDocs = EndpointDocs().withTags(List(bookTag, Tag("Another tag")))
      )
  }

  "OpenApi" should {

    val expectedSchema =
        """{
        |  "openapi": "3.0.0",
        |  "info": {
        |    "title": "TestFixturesOpenApi",
        |    "version": "0.0.0",
        |    "description": "This is a top level description."
        |  },
        |  "paths": {
        |    "/books": {
        |      "get": {
        |        "responses": {
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
        |          },
        |          "200": {
        |            "description": "Books list",
        |            "content": {
        |              "application/json": {
        |                "schema": {
        |                  "type": "array",
        |                  "items": {
        |                    "example": {
        |                      "id": "44c159ed-cb8b-464e-ad02-644c36ed0b3f",
        |                      "title": "Programming in Scala, First Edition",
        |                      "author": {
        |                        "name": "Martin Odersky, Lex Spoon, and Bill Venners"
        |                      },
        |                      "isbnCodes": [
        |
        |                      ],
        |                      "storage": {
        |                        "link": "https://www.artima.com/pins1ed/",
        |                        "storageType": "Online"
        |                      }
        |                    },
        |                    "allOf": [
        |                      {
        |                        "$ref": "#/components/schemas/endpoints4s.openapi.ReferencedSchemaTest.Book"
        |                      }
        |                    ]
        |                  }
        |                }
        |              }
        |            }
        |          }
        |        },
        |        "tags": [
        |          "Books"
        |        ]
        |      },
        |      "post": {
        |        "responses": {
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
        |          },
        |          "200": {
        |            "description": "",
        |            "content": {
        |              "application/json": {
        |                "schema": {
        |                  "example": "Blue",
        |                  "allOf": [
        |                    {
        |                      "$ref": "#/components/schemas/Color"
        |                    }
        |                  ]
        |                }
        |              }
        |            }
        |          },
        |          "403": {
        |            "description": ""
        |          }
        |        },
        |        "requestBody": {
        |          "content": {
        |            "application/json": {
        |              "schema": {
        |                "example": {
        |                  "id": "2cb38bf2-ecbf-401c-be37-9ec325ae91a0",
        |                  "title": "Essential Scala",
        |                  "author": {
        |                    "name": "Noel Welsh and Dave Gurnell"
        |                  },
        |                  "isbnCodes": [
        |
        |                  ],
        |                  "storage": {
        |                    "link": "https://underscore.io/books/essential-scala/",
        |                    "storageType": "Online"
        |                  }
        |                },
        |                "allOf": [
        |                  {
        |                    "$ref": "#/components/schemas/endpoints4s.openapi.ReferencedSchemaTest.Book"
        |                  }
        |                ]
        |              }
        |            }
        |          },
        |          "description": "Books list"
        |        },
        |        "tags": [
        |          "Books",
        |          "Another tag"
        |        ],
        |        "security": [
        |          {
        |            "HttpBasic": [
        |
        |            ]
        |          }
        |        ]
        |      }
        |    }
        |  },
        |  "tags": [
        |    {
        |      "name": "Books",
        |      "description": "A book is something you can read.",
        |      "externalDocs": {
        |        "url": "moreinfo@books.nl",
        |        "description": "The official website about books."
        |      }
        |    }
        |  ],
        |  "components": {
        |    "schemas": {
        |      "endpoints4s.openapi.ReferencedSchemaTest.Storage.Online": {
        |        "type": "object",
        |        "properties": {
        |          "storageType": {
        |            "example": "Online",
        |            "type": "string",
        |            "enum": [
        |              "Online"
        |            ]
        |          },
        |          "link": {
        |            "type": "string"
        |          }
        |        },
        |        "required": [
        |          "storageType",
        |          "link"
        |        ]
        |      },
        |      "endpoints4s.openapi.ReferencedSchemaTest.Storage": {
        |        "oneOf": [
        |          {
        |            "$ref": "#/components/schemas/endpoints4s.openapi.ReferencedSchemaTest.Storage.Library"
        |          },
        |          {
        |            "$ref": "#/components/schemas/endpoints4s.openapi.ReferencedSchemaTest.Storage.Online"
        |          }
        |        ],
        |        "discriminator": {
        |          "propertyName": "storageType",
        |          "mapping": {
        |            "Library": "#/components/schemas/endpoints4s.openapi.ReferencedSchemaTest.Storage.Library",
        |            "Online": "#/components/schemas/endpoints4s.openapi.ReferencedSchemaTest.Storage.Online"
        |          }
        |        }
        |      },
        |      "endpoints4s.openapi.ReferencedSchemaTest.Storage.Library": {
        |        "type": "object",
        |        "properties": {
        |          "storageType": {
        |            "example": "Library",
        |            "type": "string",
        |            "enum": [
        |              "Library"
        |            ]
        |          },
        |          "room": {
        |            "type": "string"
        |          },
        |          "shelf": {
        |            "type": "integer",
        |            "format": "int32"
        |          }
        |        },
        |        "required": [
        |          "storageType",
        |          "room",
        |          "shelf"
        |        ]
        |      },
        |      "Color": {
        |        "example": "Blue",
        |        "type": "string",
        |        "enum": [
        |          "Red",
        |          "Blue"
        |        ]
        |      },
        |      "endpoints.Errors": {
        |        "type": "array",
        |        "items": {
        |          "type": "string"
        |        }
        |      },
        |      "endpoints4s.openapi.ReferencedSchemaTest.Book": {
        |        "example": {
        |          "id": "2cb38bf2-ecbf-401c-be37-9ec325ae91a0",
        |          "title": "Essential Scala",
        |          "author": {
        |            "name": "Noel Welsh and Dave Gurnell"
        |          },
        |          "isbnCodes": [
        |
        |          ],
        |          "storage": {
        |            "link": "https://underscore.io/books/essential-scala/",
        |            "storageType": "Online"
        |          }
        |        },
        |        "type": "object",
        |        "properties": {
        |          "id": {
        |            "description": "Universally unique identifier (RFC 4122)",
        |            "example": "5f27b818-027a-4008-b410-de01e1dd3a93",
        |            "type": "string",
        |            "format": "uuid"
        |          },
        |          "title": {
        |            "type": "string"
        |          },
        |          "author": {
        |            "type": "object",
        |            "properties": {
        |              "name": {
        |                "description": "Author name",
        |                "type": "string"
        |              }
        |            },
        |            "required": [
        |              "name"
        |            ]
        |          },
        |          "isbnCodes": {
        |            "type": "array",
        |            "items": {
        |              "type": "string"
        |            }
        |          },
        |          "storage": {
        |            "$ref": "#/components/schemas/endpoints4s.openapi.ReferencedSchemaTest.Storage"
        |          }
        |        },
        |        "required": [
        |          "id",
        |          "title",
        |          "author",
        |          "isbnCodes",
        |          "storage"
        |        ]
        |      }
        |    },
        |    "securitySchemes": {
        |      "HttpBasic": {
        |        "type": "http",
        |        "description": "Http Basic Authentication",
        |        "scheme": "basic"
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
