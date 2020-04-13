package endpoints.openapi

import endpoints.algebra
import org.scalatest.OptionValues
import endpoints.openapi.model._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.util.Random

//TODO cover tests from algebra package
class EndpointsTest extends AnyWordSpec with Matchers with OptionValues {

  "Path parameters" should {
    "Appear as patterns between braces in the documentation" in {
      Fixtures.baz.path shouldBe "/baz/{quux}"
      Fixtures.multipleSegmentsPath.path shouldBe "/assets/{file}"
    }
  }

  "Query parameters" should {
    "Have a correct schema" in {
      val expectedParameters =
        Parameter(
          "n",
          In.Query,
          required = true,
          description = None,
          schema = Schema.simpleNumber
        ) ::
          Parameter(
            "lang",
            In.Query,
            required = false,
            description = None,
            schema = Schema.simpleString
          ) ::
          Parameter(
            "ids",
            In.Query,
            required = false,
            description = None,
            schema = Schema.Array(Left(Schema.simpleInteger), None, None, None)
          ) ::
          Nil
      Fixtures.quux.item
        .operations("get")
        .parameters shouldBe expectedParameters
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
          Schema.Property(
            "name",
            Schema.Primitive("string", None, None, None, None),
            isRequired = true,
            description = Some("Name of the user")
          ) ::
            Schema.Property(
              "age",
              Schema.Primitive("integer", Some("int32"), None, None, None),
              isRequired = true,
              description = None
            ) ::
            Nil,
          None,
          None,
          None,
          None
        )
      Fixtures.toSchema(Fixtures.User.schema.docs) shouldBe expectedSchema
    }
  }

  "Enumerations" in {
    val expectedSchema =
      Schema.Reference(
        "Color",
        Some(
          Schema.Enum(
            Schema.Primitive("string", None, None, None, None),
            ujson.Str("Red") :: ujson.Str("Blue") :: Nil,
            None,
            None,
            None
          )
        ),
        None,
        None,
        None
      )
    Fixtures.toSchema(Fixtures.Enum.colorSchema.docs) shouldBe expectedSchema
  }

  "Recursive types" in {
    val recSchema =
      Schema.Reference(
        "Rec",
        Some(
          Schema.Object(
            Schema.Property(
              "next",
              Schema.Reference("Rec", None, None),
              isRequired = false,
              description = None
            ) :: Nil,
            additionalProperties = None,
            description = None,
            example = None,
            title = None
          )
        ),
        None,
        None,
        None
      )
    val expectedSchema =
      Schema.Object(
        Schema.Property(
          "next",
          recSchema,
          isRequired = false,
          description = None
        ) :: Nil,
        additionalProperties = None,
        description = None,
        example = None,
        title = None
      )
    Fixtures.toSchema(Fixtures.recursiveSchema.docs) shouldBe expectedSchema
  }

  "Refining JSON schemas preserves documentation" should {
    "JsonSchema" in {
      val expectedSchema = Fixtures.intJsonSchema.docs
      Fixtures.evenNumberSchema.docs shouldBe expectedSchema
    }
    "Tagged" in {
      val expectedSchema = Fixtures.Foo.schema.docs
      Fixtures.refinedTaggedSchema.docs shouldBe expectedSchema
    }
  }

  "Text response" should {
    "be properly encoded" in {
      val reqBody = Fixtures.documentation
        .paths("/textRequestEndpoint")
        .operations("post")
        .requestBody

      reqBody shouldBe defined
      reqBody.value.description.value shouldEqual "Text Req"
      reqBody.value.content("text/plain").schema.value shouldEqual Schema
        .Primitive("string", None, None, None, None)
    }
  }

  "Empty segment name" should {
    "be substituted with auto generated name" in {
      val path = Fixtures.documentation.paths
        .find(_._1.startsWith("/emptySegmentNameEndp"))
        .get

      path._1 shouldEqual "/emptySegmentNameEndp/{_arg0}/x/{_arg1}"
      val pathParams = path._2.operations("post").parameters
      pathParams(0).name shouldEqual "_arg0"
      pathParams(1).name shouldEqual "_arg1"
    }
  }

  "Tags documentation" should {
    "be set according to provided tags" in {
      Fixtures.documentation
        .paths("/foo")
        .operations("get")
        .tags shouldEqual List("foo")
      Fixtures.documentation
        .paths("/foo")
        .operations("post")
        .tags shouldEqual List("bar", "bxx")
      Fixtures.documentation.paths
        .find(_._1.startsWith("/baz"))
        .get
        ._2
        .operations("get")
        .tags shouldEqual List("baz", "bxx")
    }
  }

  "Response headers" should {
    "be documented" in {
      val expectedHeader = Map(
        "ETag" -> ResponseHeader(
          required = true,
          description = Some("version number"),
          Schema.simpleString
        )
      )
      val headers = Fixtures.documentation
        .paths("/versioned-resource")
        .operations("get")
        .responses("200")
        .headers
      headers shouldEqual expectedHeader
    }
  }

  "Deprecation documentation" should {
    "be set according to provided docs" in {
      Fixtures.documentation
        .paths("/textRequestEndpoint")
        .operations("post")
        .deprecated shouldBe true
    }
  }

  "Descriptions and summary documentation" should {
    "be rendered according to provided docs" in {
      import Fixtures.{openApi, documentedEndp}

      val docs = openApi(Info("test", "0.0.0"))(documentedEndp)
      val json = ujson.read(OpenApi.stringEncoder.encode(docs))
      val documentedEndpJson = json("paths")("/documented")("get")

      documentedEndpJson("summary") shouldBe ujson.Str("summary of endpoint")
      documentedEndpJson("description") shouldBe ujson.Str(
        "description of endpoint"
      )
    }
  }

  "Fields order" in {
    import Fixtures.{
      emptyRecord,
      endpoint,
      field,
      get,
      jsonResponse,
      ok,
      path,
      openApi,
      Record,
      Request,
      Response
    }
    for {
      // Run the test 50 times
      _ <- 1 to 50
      // Create an object schema with 15 properties
      length = 15
      // Property names are random
      keys = List.fill(length)(Random.nextString(10))
      schema = keys
        .foldLeft[Record[_]](emptyRecord)((schema, key) =>
          schema.zip(field[Int](key))
        )
        .named("Resource")
      item = endpoint(
        get(path): Request[Unit],
        ok(jsonResponse(schema)): Response[Record[_]]
      )
      docs = openApi(Info("test", "0.0.0"))(item)
      json = ujson.read(OpenApi.stringEncoder.encode(docs))
    } {
      json("components")("schemas")("Resource")("properties").obj.toList shouldBe (
        keys.map(key =>
          (
            key,
            ujson.Obj(
              "type" -> ujson.Str("integer"),
              "format" -> ujson.Str("int32")
            )
          )
        )
      )
    }
  }

}

trait Fixtures extends algebra.Endpoints with algebra.ChunkedEntities {

  val foo = endpoint(
    get(path / "foo"),
    ok(emptyResponse, Some("Foo response")),
    docs = EndpointDocs(tags = List("foo"))
  )

  val bar = endpoint(
    post(path / "foo", emptyRequest),
    ok(emptyResponse, Some("Bar response")),
    docs = EndpointDocs(tags = List("bar", "bxx"))
  )

  val baz = endpoint(
    get(path / "baz" / segment[Int]("quux")),
    ok(emptyResponse, Some("Baz response")),
    docs = EndpointDocs(tags = List("baz", "bxx"))
  )

  val textRequestEndp = endpoint(
    post(path / "textRequestEndpoint", textRequest, docs = Some("Text Req")),
    ok(emptyResponse),
    docs = EndpointDocs(deprecated = true)
  )

  val emptySegmentNameEndp = endpoint(
    post(
      path / "emptySegmentNameEndp" / segment[Int]() / "x" / segment[String](),
      textRequest
    ),
    ok(emptyResponse)
  )

  val quux = endpoint(
    get(
      path / "quux" /? (qs[Double]("n") & qs[Option[String]]("lang") & qs[List[
        Long
      ]]("ids"))
    ),
    ok(emptyResponse)
  )

  val multipleSegmentsPath =
    endpoint(get(path / "assets" / remainingSegments("file")), ok(textResponse))

  val assets =
    endpoint(
      get(path / "assets2" / remainingSegments("file")),
      ok(bytesChunksResponse)
    )

  val versionedResource =
    endpoint(
      get(path / "versioned-resource"),
      ok(textResponse, headers = responseHeader("ETag", Some("version number")))
    )

  val documentedEndp = endpoint(
    request = get(path / "documented"),
    response = ok(emptyResponse),
    docs = EndpointDocs(
      summary = Some("summary of endpoint"),
      description = Some("description of endpoint")
    )
  )
}

object Fixtures
    extends Fixtures
    with algebra.JsonSchemasFixtures
    with Endpoints
    with JsonEntitiesFromSchemas
    with ChunkedEntities {

  val documentation =
    openApi(Info("Test API", "1.0.0"))(
      foo,
      bar,
      baz,
      textRequestEndp,
      emptySegmentNameEndp,
      quux,
      assets,
      versionedResource
    )

}
