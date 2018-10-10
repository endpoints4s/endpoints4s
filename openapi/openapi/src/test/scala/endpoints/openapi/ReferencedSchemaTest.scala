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

  case class Book(id: Int, title: String, author: String, isbnCodes: List[String], storage: Storage)

  object Fixtures extends Fixtures with openapi.Endpoints with openapi.JsonSchemaEntities {

    def openApi: OpenApi = openApi(
      Info(title = "TestFixturesOpenApi", version = "0.0.0")
    )(Fixtures.listBooks, Fixtures.postBook)
  }

  trait Fixtures extends algebra.Endpoints with algebra.JsonSchemaEntities with generic.JsonSchemas {

    implicit private val schemaStorage: JsonSchema[Storage] =
      withDiscriminator(genericJsonSchema[Storage].asInstanceOf[Tagged[Storage]], "storageType")

    implicit private val schemaBook: JsonSchema[Book] = genericJsonSchema[Book]

    val listBooks = endpoint(get(path / "books"), jsonResponse[List[Book]](Some("Books list")), tags = List("Books"))

    val postBook = endpoint(post(path / "books", jsonRequest[Book](docs = Some("Books list"))), emptyResponse(), tags = List("Books"))
  }

  "OpenApi" should {

    "produce referenced schema" in {

      import io.circe.syntax._

      Fixtures.openApi.asJson.spaces2 shouldBe
        """{
          |  "components" : {
          |    "schemas" : {
          |      "endpoints.openapi.ReferencedSchemaTest.Book" : {
          |        "required" : [
          |          "id",
          |          "title",
          |          "author",
          |          "isbnCodes",
          |          "storage"
          |        ],
          |        "type" : "object",
          |        "properties" : {
          |          "id" : {
          |            "type" : "integer",
          |            "format" : "int32"
          |          },
          |          "title" : {
          |            "type" : "string"
          |          },
          |          "author" : {
          |            "type" : "string"
          |          },
          |          "isbnCodes" : {
          |            "type" : "array",
          |            "items" : {
          |              "type" : "string"
          |            }
          |          },
          |          "storage" : {
          |            "$ref" : "#/components/schemas/endpoints.openapi.ReferencedSchemaTest.Storage"
          |          }
          |        }
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
          |      "endpoints.openapi.ReferencedSchemaTest.Storage.Library" : {
          |        "allOf" : [
          |          {
          |            "$ref" : "#/components/schemas/endpoints.openapi.ReferencedSchemaTest.Storage"
          |          },
          |          {
          |            "required" : [
          |              "storageType",
          |              "room",
          |              "shelf"
          |            ],
          |            "type" : "object",
          |            "properties" : {
          |              "storageType" : {
          |                "type" : "string"
          |              },
          |              "room" : {
          |                "type" : "string"
          |              },
          |              "shelf" : {
          |                "type" : "integer",
          |                "format" : "int32"
          |              }
          |            }
          |          }
          |        ]
          |      },
          |      "endpoints.openapi.ReferencedSchemaTest.Storage.Online" : {
          |        "allOf" : [
          |          {
          |            "$ref" : "#/components/schemas/endpoints.openapi.ReferencedSchemaTest.Storage"
          |          },
          |          {
          |            "required" : [
          |              "storageType",
          |              "link"
          |            ],
          |            "type" : "object",
          |            "properties" : {
          |              "storageType" : {
          |                "type" : "string"
          |              },
          |              "link" : {
          |                "type" : "string"
          |              }
          |            }
          |          }
          |        ]
          |      }
          |    }
          |  },
          |  "openapi" : "3.0.0",
          |  "info" : {
          |    "title" : "TestFixturesOpenApi",
          |    "version" : "0.0.0"
          |  },
          |  "paths" : {
          |    "/books" : {
          |      "get" : {
          |        "responses" : {
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
          |        "responses" : {
          |          "200" : {
          |            "description" : ""
          |          }
          |        },
          |        "requestBody" : {
          |          "description" : "Books list",
          |          "content" : {
          |            "application/json" : {
          |              "schema" : {
          |                "$ref" : "#/components/schemas/endpoints.openapi.ReferencedSchemaTest.Book"
          |              }
          |            }
          |          }
          |        },
          |        "tags" : [
          |          "Books"
          |        ]
          |      }
          |    }
          |  }
          |}""".stripMargin
    }
  }
}
