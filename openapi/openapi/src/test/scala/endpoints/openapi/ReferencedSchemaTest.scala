package endpoints.openapi

import endpoints.openapi.model._
import endpoints.{algebra, generic, openapi}
import org.scalatest.{Matchers, WordSpec}

class ReferencedSchemaTest extends WordSpec with Matchers {

  case class Book(id: Int, title: String, author: String, isbnCodes: List[String])

  object Fixtures extends Fixtures with openapi.Endpoints with openapi.JsonSchemaEntities {

    def openApi: OpenApi = openApi(
      Info(title = "TestFixturesOpenApi", version = "0.0.0")
    )(Fixtures.listBooks, Fixtures.postBook)
  }

  trait Fixtures extends algebra.Endpoints with algebra.JsonSchemaEntities with generic.JsonSchemas {

    implicit private val schemaBook: JsonSchema[Book] = genericJsonSchema[Book]

    val listBooks = endpoint(get(path / "books"), jsonResponse[List[Book]](Some("Books list")), tags = List("Books"))

    val postBook = endpoint(post(path / "books", jsonRequest[Book](docs = Some("Books list"))), emptyResponse(), tags = List("Books"))
  }

  "OpenApi" should {

    "produce referenced schema" in {

      import io.circe.syntax._

      Fixtures.openApi.asJson.spaces2 shouldBe
        """{
          |  "openapi" : "3.0.0",
          |  "info" : {
          |    "title" : "TestFixturesOpenApi",
          |    "version" : "0.0.0"
          |  },
          |  "paths" : {
          |    "/books" : {
          |      "get" : {
          |        "parameters" : [
          |        ],
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
          |        "parameters" : [
          |        ],
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
          |  },
          |  "components" : {
          |    "schemas" : {
          |      "endpoints.openapi.ReferencedSchemaTest.Book" : {
          |        "required" : [
          |          "id",
          |          "title",
          |          "author",
          |          "isbnCodes"
          |        ],
          |        "type" : "object",
          |        "properties" : {
          |          "id" : {
          |            "type" : "integer"
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
          |          }
          |        }
          |      }
          |    }
          |  }
          |}""".stripMargin
    }
  }
}
