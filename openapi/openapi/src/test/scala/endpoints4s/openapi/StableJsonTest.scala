package endpoints4s.openapi

import endpoints4s.openapi.model._
import endpoints4s.{algebra, generic, openapi}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class StableJsonTest extends AnyWordSpec with Matchers {
  import StableJsonTest._

  "OpenApi json" should {
    "order components by name" in {
      val expectedSchema =
        """{
          |  "openapi": "3.0.0",
          |  "info": {
          |    "title": "TestFixturesOpenApi",
          |    "version": "0.0.0",
          |    "description": "This is a top level description."
          |  },
          |  "paths": {
          |    "/books2": {
          |      "post": {
          |        "responses": {
          |          "200": {
          |            "description": "",
          |            "content": {
          |              "application/json": {
          |                "schema": {
          |                  "$ref": "#/components/schemas/endpoints4s.openapi.StableJsonTest.Author"
          |                }
          |              }
          |            }
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
          |          "403": {
          |            "description": ""
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
          |        "requestBody": {
          |          "required": true,
          |          "content": {
          |            "application/json": {
          |              "schema": {
          |                "$ref": "#/components/schemas/endpoints4s.openapi.StableJsonTest.Book"
          |              }
          |            }
          |          }
          |        },
          |        "security": [
          |          {
          |            "HttpBasic": []
          |          }
          |        ]
          |      }
          |    },
          |    "/books": {
          |      "post": {
          |        "responses": {
          |          "200": {
          |            "description": "",
          |            "content": {
          |              "application/json": {
          |                "schema": {
          |                  "$ref": "#/components/schemas/endpoints4s.openapi.StableJsonTest.Author"
          |                }
          |              }
          |            }
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
          |          "403": {
          |            "description": ""
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
          |        "requestBody": {
          |          "required": true,
          |          "content": {
          |            "application/json": {
          |              "schema": {
          |                "$ref": "#/components/schemas/endpoints4s.openapi.StableJsonTest.Book"
          |              }
          |            }
          |          }
          |        },
          |        "security": [
          |          {
          |            "HttpBasic": []
          |          }
          |        ]
          |      },
          |      "get": {
          |        "responses": {
          |          "200": {
          |            "description": "Books list",
          |            "content": {
          |              "application/json": {
          |                "schema": {
          |                  "type": "array",
          |                  "items": {
          |                    "$ref": "#/components/schemas/endpoints4s.openapi.StableJsonTest.Book"
          |                  }
          |                }
          |              }
          |            }
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
          |        }
          |      }
          |    }
          |  },
          |  "components": {
          |    "schemas": {
          |      "endpoints.Errors": {
          |        "type": "array",
          |        "items": {
          |          "type": "string"
          |        }
          |      },
          |      "endpoints4s.openapi.StableJsonTest.Author": {
          |        "type": "object",
          |        "properties": {
          |          "name": {
          |            "type": "string"
          |          }
          |        },
          |        "required": [
          |          "name"
          |        ]
          |      },
          |      "endpoints4s.openapi.StableJsonTest.Book": {
          |        "type": "object",
          |        "properties": {
          |          "title": {
          |            "type": "string"
          |          }
          |        },
          |        "required": [
          |          "title"
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
      val actual =
        ujson.read(OpenApi.stringEncoder.encode(Fixtures.openApiDocument))
      actual.render(2) shouldBe expectedSchema
    }
    "preserve operations order" in {
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
          |          "200": {
          |            "description": "Books list",
          |            "content": {
          |              "application/json": {
          |                "schema": {
          |                  "type": "array",
          |                  "items": {
          |                    "$ref": "#/components/schemas/endpoints4s.openapi.StableJsonTest.Book"
          |                  }
          |                }
          |              }
          |            }
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
          |        }
          |      },
          |      "post": {
          |        "responses": {
          |          "200": {
          |            "description": "",
          |            "content": {
          |              "application/json": {
          |                "schema": {
          |                  "$ref": "#/components/schemas/endpoints4s.openapi.StableJsonTest.Author"
          |                }
          |              }
          |            }
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
          |          "403": {
          |            "description": ""
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
          |        "requestBody": {
          |          "required": true,
          |          "content": {
          |            "application/json": {
          |              "schema": {
          |                "$ref": "#/components/schemas/endpoints4s.openapi.StableJsonTest.Book"
          |              }
          |            }
          |          }
          |        },
          |        "security": [
          |          {
          |            "HttpBasic": []
          |          }
          |        ]
          |      }
          |    },
          |    "/books2": {
          |      "post": {
          |        "responses": {
          |          "200": {
          |            "description": "",
          |            "content": {
          |              "application/json": {
          |                "schema": {
          |                  "$ref": "#/components/schemas/endpoints4s.openapi.StableJsonTest.Author"
          |                }
          |              }
          |            }
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
          |          "403": {
          |            "description": ""
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
          |        "requestBody": {
          |          "required": true,
          |          "content": {
          |            "application/json": {
          |              "schema": {
          |                "$ref": "#/components/schemas/endpoints4s.openapi.StableJsonTest.Book"
          |              }
          |            }
          |          }
          |        },
          |        "security": [
          |          {
          |            "HttpBasic": []
          |          }
          |        ]
          |      }
          |    }
          |  },
          |  "components": {
          |    "schemas": {
          |      "endpoints.Errors": {
          |        "type": "array",
          |        "items": {
          |          "type": "string"
          |        }
          |      },
          |      "endpoints4s.openapi.StableJsonTest.Author": {
          |        "type": "object",
          |        "properties": {
          |          "name": {
          |            "type": "string"
          |          }
          |        },
          |        "required": [
          |          "name"
          |        ]
          |      },
          |      "endpoints4s.openapi.StableJsonTest.Book": {
          |        "type": "object",
          |        "properties": {
          |          "title": {
          |            "type": "string"
          |          }
          |        },
          |        "required": [
          |          "title"
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
      val actual =
        ujson.read(OpenApi.stringEncoder.encode(Fixtures.openApiDocument2))
      actual.render(2) shouldBe expectedSchema
    }
  }
}

object StableJsonTest {
  case class Book(title: String)
  case class Author(name: String)

  object Fixtures
    extends Fixtures
      with openapi.Endpoints
      with openapi.JsonEntitiesFromSchemas
      with openapi.BasicAuthentication {

    def openApiDocument: OpenApi =
      openApi(
        Info(title = "TestFixturesOpenApi", version = "0.0.0")
          .withDescription(Some("This is a top level description."))
      )(Fixtures.postBook2, Fixtures.postBook, Fixtures.listBooks)
    def openApiDocument2: OpenApi =
      openApi(
        Info(title = "TestFixturesOpenApi", version = "0.0.0")
          .withDescription(Some("This is a top level description."))
      )(Fixtures.listBooks, Fixtures.postBook, Fixtures.postBook2)
  }

  trait Fixtures
    extends algebra.Endpoints
      with algebra.JsonEntitiesFromSchemas
      with generic.JsonSchemas
      with algebra.BasicAuthentication
      with algebra.JsonSchemasFixtures {

    implicit val schemaBook: JsonSchema[Book] = genericJsonSchema[Book]
    implicit val schemaAuthor: JsonSchema[Author] = genericJsonSchema[Author]

    val listBooks = endpoint(
      get(path / "books"),
      ok(jsonResponse[List[Book]], Some("Books list")),
    )

    val postBook =
      authenticatedEndpoint(
        Post,
        path / "books",
        ok(jsonResponse[Author]),
        jsonRequest[Book],
      )

    val postBook2 =
      authenticatedEndpoint(
        Post,
        path / "books2",
        ok(jsonResponse[Author]),
        jsonRequest[Book],
      )
  }
}
