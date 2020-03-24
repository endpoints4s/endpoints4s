package endpoints.openapi

import endpoints.algebra
import endpoints.openapi.model._
import org.scalatest.freespec.AnyFreeSpec

class CoproductEncodingTest extends AnyFreeSpec {

  trait CoproductEncodingAlg
      extends algebra.Endpoints
      with algebra.JsonEntitiesFromSchemas
      with algebra.JsonSchemasFixtures {

    implicit val fooSchema = Foo.schema.named("Foo")

    val foo = endpoint(get(path / "foo"), ok(jsonResponse[Foo]))
  }

  object OneOfStrategyDocs
      extends CoproductEncodingAlg
      with Endpoints
      with JsonEntitiesFromSchemas {

    override def coproductEncoding = CoproductEncoding.OneOf
    val api = openApi(Info("OneOf", "1"))(foo)
  }

  object OneOfWithBaseRefStrategyDocs
      extends CoproductEncodingAlg
      with Endpoints
      with JsonEntitiesFromSchemas {

    override def coproductEncoding = CoproductEncoding.OneOfWithBaseRef
    val api = openApi(Info("OneOf", "1"))(foo)
  }

  "OneOf schema encoding" in {
    val expectedSchema = Schema.OneOf(
      Schema.DiscriminatedAlternatives(
        "type",
        List(
          "Bar" -> Schema.Object(
            List(
              Schema.Property(
                "type",
                Schema.Enum(
                  Schema.simpleString,
                  List("Bar"),
                  None,
                  Some("Bar"),
                  None
                ),
                true,
                None
              ),
              Schema.Property("s", Schema.simpleString, true, None)
            ),
            None,
            None,
            None,
            None
          ),
          "Baz" -> Schema.Object(
            List(
              Schema.Property(
                "type",
                Schema.Enum(
                  Schema.simpleString,
                  List("Baz"),
                  None,
                  Some("Baz"),
                  None
                ),
                true,
                None
              ),
              Schema.Property(
                "i",
                Schema.Primitive("integer", Some("int32"), None, None, None),
                true,
                None
              )
            ),
            None,
            None,
            None,
            None
          ),
          "Bax" -> Schema.Object(
            List(
              Schema.Property(
                "type",
                Schema.Enum(
                  Schema.simpleString,
                  List("Bax"),
                  None,
                  Some("Bax"),
                  None
                ),
                true,
                None
              )
            ),
            None,
            None,
            None,
            None
          ),
          "Qux" -> Schema.Object(
            List(
              Schema.Property(
                "type",
                Schema.Enum(
                  Schema.simpleString,
                  List("Qux"),
                  None,
                  Some("Qux"),
                  None
                ),
                true,
                None
              )
            ),
            None,
            None,
            None,
            None
          ),
          "Quux" -> Schema.Object(
            List(
              Schema.Property(
                "type",
                Schema.Enum(
                  Schema.simpleString,
                  List("Quux"),
                  None,
                  Some("Quux"),
                  None
                ),
                true,
                None
              ),
              Schema.Property("b", Schema.simpleInteger, true, None)
            ),
            None,
            None,
            None,
            None
          )
        )
      ),
      None,
      None,
      None
    )
    assert(OneOfStrategyDocs.api.components.schemas("Foo") == expectedSchema)
  }

  "OneOfWithBaseRef schema encoding" in {
    val expectedSchema = Schema.OneOf(
      Schema.DiscriminatedAlternatives(
        "type",
        List(
          "Bar" -> Schema.AllOf(
            List(
              Schema.Reference("Foo", None, None, None, None),
              Schema.Object(
                List(
                  Schema.Property(
                    "type",
                    Schema.Enum(
                      Schema.simpleString,
                      List("Bar"),
                      None,
                      Some("Bar"),
                      None
                    ),
                    true,
                    None
                  ),
                  Schema.Property("s", Schema.simpleString, true, None)
                ),
                None,
                None,
                None,
                None
              )
            ),
            None,
            None,
            None
          ),
          "Baz" -> Schema.AllOf(
            List(
              Schema.Reference("Foo", None, None, None, None),
              Schema.Object(
                List(
                  Schema.Property(
                    "type",
                    Schema.Enum(
                      Schema.simpleString,
                      List("Baz"),
                      None,
                      Some("Baz"),
                      None
                    ),
                    true,
                    None
                  ),
                  Schema.Property(
                    "i",
                    Schema
                      .Primitive("integer", Some("int32"), None, None, None),
                    true,
                    None
                  )
                ),
                None,
                None,
                None,
                None
              )
            ),
            None,
            None,
            None
          ),
          "Bax" -> Schema.AllOf(
            List(
              Schema.Reference("Foo", None, None, None, None),
              Schema.Object(
                List(
                  Schema.Property(
                    "type",
                    Schema.Enum(
                      Schema.simpleString,
                      List("Bax"),
                      None,
                      Some("Bax"),
                      None
                    ),
                    true,
                    None
                  )
                ),
                None,
                None,
                None,
                None
              )
            ),
            None,
            None,
            None
          ),
          "Qux" -> Schema.AllOf(
            List(
              Schema.Reference("Foo", None, None, None, None),
              Schema.Object(
                List(
                  Schema.Property(
                    "type",
                    Schema.Enum(
                      Schema.simpleString,
                      List("Qux"),
                      None,
                      Some("Qux"),
                      None
                    ),
                    true,
                    None
                  )
                ),
                None,
                None,
                None,
                None
              )
            ),
            None,
            None,
            None
          ),
          "Quux" -> Schema.AllOf(
            List(
              Schema.Reference("Foo", None, None, None, None),
              Schema.Object(
                List(
                  Schema.Property(
                    "type",
                    Schema.Enum(
                      Schema.simpleString,
                      List("Quux"),
                      None,
                      Some("Quux"),
                      None
                    ),
                    true,
                    None
                  ),
                  Schema.Property("b", Schema.simpleInteger, true, None)
                ),
                None,
                None,
                None,
                None
              )
            ),
            None,
            None,
            None
          )
        )
      ),
      None,
      None,
      None
    )
    assert(
      OneOfWithBaseRefStrategyDocs.api.components
        .schemas("Foo") == expectedSchema
    )
  }
}
