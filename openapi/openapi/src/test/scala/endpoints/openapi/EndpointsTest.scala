package endpoints.openapi

import endpoints.algebra
import org.scalatest.{Matchers, OptionValues, WordSpec}
import endpoints.openapi.model._

//TODO cover tests from algebra package
class EndpointsTest extends WordSpec with Matchers with OptionValues {

  "Path parameters" should {
    "Appear as patterns between braces in the documentation" in {
      Fixtures.baz.path shouldBe "/baz/{quux}"
    }
  }

  "Endpoints sharing the same path" should {
    "be grouped together" in {

      val fooItem =
        Fixtures.documentation.paths.get("/foo").value

      fooItem.operations.size shouldBe 2
      fooItem.operations.get("get") shouldBe defined
      fooItem.operations.get("post") shouldBe defined

    }
  }

  "Fields documentation" should {
    "be exposed in JSON schema" in {
      val expectedSchema =
        Schema.Object(
          Schema.Property("name", Schema.Primitive("string"), isRequired = true, description = Some("Name of the user")) ::
          Schema.Property("age", Schema.Primitive("integer"), isRequired = true, description = None) ::
          Nil,
          None
        )
      Fixtures.toSchema(Fixtures.User.schema) shouldBe expectedSchema
    }
  }

  "Text response" should {
    "be properly encoded" in {
      val reqBody = Fixtures.documentation.paths("/textRequestEndpoint").operations("post").requestBody

      reqBody shouldBe defined
      reqBody.value.description.value shouldEqual "Text Req"
      reqBody.value.content("text/plain").schema.value shouldEqual Schema.Primitive("string")
    }
  }

  "Empty segment name" should {
    "be substituted with auto generated name" in {
      val path = Fixtures.documentation.paths.find(_._1.startsWith("/emptySegmentNameEndp")).get

      path._1 shouldEqual "/emptySegmentNameEndp/{_arg0}/x/{_arg1}"
      val pathParams = path._2.operations("post").parameters
      pathParams(0).name shouldEqual "_arg0"
      pathParams(1).name shouldEqual "_arg1"
    }
  }

}

trait Fixtures extends algebra.Endpoints {

  val foo = endpoint(get(path / "foo"), emptyResponse(Some("Foo response")))

  val bar = endpoint(post(path / "foo", emptyRequest), emptyResponse(Some("Bar response")))

  val baz = endpoint(get(path / "baz" / segment[Int]("quux")), emptyResponse(Some("Baz response")))

  val textRequestEndp = endpoint(post(path / "textRequestEndpoint", textRequest(docs = Some("Text Req"))), emptyResponse())

  val emptySegmentNameEndp = endpoint(post(path / "emptySegmentNameEndp" / segment[Int]() / "x" / segment[String](), textRequest()), emptyResponse())

}

object Fixtures
  extends Fixtures
    with algebra.JsonSchemasTest
    with Endpoints
    with JsonSchemaEntities {

  val documentation = openApi(Info("Test API", "1.0.0"))(foo, bar, baz, textRequestEndp,emptySegmentNameEndp)

}