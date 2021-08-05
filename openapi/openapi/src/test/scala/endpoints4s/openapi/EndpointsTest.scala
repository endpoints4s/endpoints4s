package endpoints4s.openapi

import endpoints4s.algebra
import endpoints4s.algebra.{ExternalDocumentationObject, Tag}
import org.scalatest.OptionValues
import endpoints4s.openapi.model._
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
            schema = Schema.Array(Left(Schema.simpleLong), None, None, None)
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
        None
      )
    Fixtures.toSchema(Fixtures.Enum.colorSchema.docs) shouldBe expectedSchema
  }

  "Recursive types" in {
    val expectedSchema =
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
            description = Some("Rec description"),
            example = Some(ujson.Obj()),
            title = Some("Rec title")
          )
        ),
        None
      )
    Fixtures.toSchema(Fixtures.recursiveSchema.docs) shouldBe expectedSchema
  }

  "Recursive expression" in {
    val expectedSchema =
      Schema.Reference(
        "Expression",
        Some(
          Schema.OneOf(
            Schema.EnumeratedAlternatives(
              Schema.Primitive("integer", Some("int32"), None, None, None) ::
                Schema.Object(
                  Schema.Property(
                    "x",
                    Schema.Reference("Expression", None, None),
                    isRequired = true,
                    description = None
                  ) :: Schema.Property(
                    "y",
                    Schema.Reference("Expression", None, None),
                    isRequired = true,
                    description = None
                  ) :: Nil,
                  additionalProperties = None,
                  description = None,
                  example = None,
                  title = None
                ) :: Nil
            ),
            Some("Expression description"),
            Some(ujson.Num(1)),
            Some("Expression title")
          )
        ),
        None
      )
    Fixtures.toSchema(Fixtures.expressionSchema.docs) shouldBe expectedSchema
  }
  "Mutually recursive types" in {
    Fixtures.toSchema(Fixtures.mutualRecursiveA.docs) shouldBe Schema.Reference(
      "MutualRecursiveA",
      Some(
        Schema.Object(
          Schema.Property(
            "b",
            Schema.Reference(
              "MutualRecursiveB",
              Some(
                Schema.Object(
                  Schema.Property(
                    "a",
                    Schema.Reference(
                      "MutualRecursiveA",
                      None,
                      None
                    ),
                    isRequired = false,
                    description = None
                  ) :: Nil,
                  additionalProperties = None,
                  description = None,
                  example = None,
                  title = None
                )
              ),
              None
            ),
            isRequired = false,
            description = None
          ) :: Nil,
          additionalProperties = None,
          description = None,
          example = None,
          title = None
        )
      ),
      None
    )
  }
  "Tagged Recursive" in {
    def kindSchema(name: String) = Schema.Property(
      "kind",
      Schema.Enum(
        Schema.Primitive("string", None, None, None, None),
        name :: Nil,
        None,
        Some(name),
        None
      ),
      isRequired = true,
      description = None
    )
    val nextSchema = Schema.Property(
      "next",
      Schema.Reference("TaggedRec", None, None),
      isRequired = false,
      description = None
    )
    Fixtures.toSchema(Fixtures.taggedRecursiveSchema.docs) shouldBe Schema.Reference(
      "TaggedRec",
      Some(
        Schema.OneOf(
          Schema.DiscriminatedAlternatives(
            "kind",
            (
              "A" -> Schema.Object(
                kindSchema("A") ::
                  Schema.Property(
                    "a",
                    Schema.Primitive("string", None, None, None, None),
                    isRequired = true,
                    description = None
                  )
                  :: nextSchema
                  :: Nil,
                additionalProperties = None,
                description = None,
                example = None,
                title = None
              )
            ) :: (
              "B" -> Schema.Object(
                kindSchema("B") ::
                  Schema.Property(
                    "b",
                    Schema.Primitive("integer", Some("int32"), None, None, None),
                    isRequired = true,
                    description = None
                  )
                  :: nextSchema
                  :: Nil,
                additionalProperties = None,
                description = None,
                example = None,
                title = None
              )
            ) :: Nil
          ),
          Some("TaggedRec description"),
          Some(ujson.Obj("a" -> ujson.Str("foo"), "kind" -> ujson.Str("A"))),
          Some("TaggedRec title")
        )
      ),
      None
    )
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

  "Operation documentation" should {
    "be set according to provided operationId" in {
      Fixtures.documentation
        .paths("/foo")
        .operations("get")
        .operationId shouldEqual Some("foo")
    }

    "not have any operationId set" in {
      Fixtures.documentation
        .paths("/foo")
        .operations("post")
        .operationId shouldEqual None
    }
  }

  "Tags documentation" should {
    "be set according to provided tags" in {
      Fixtures.documentation
        .paths("/foo")
        .operations("get")
        .tags shouldEqual List(Fixtures.fooTag)
      Fixtures.documentation
        .paths("/foo")
        .operations("post")
        .tags shouldEqual List(Fixtures.barTag, Fixtures.bxxTag)
      Fixtures.documentation.paths
        .find(_._1.startsWith("/baz"))
        .get
        ._2
        .operations("get")
        .tags shouldEqual List(Fixtures.bazTag, Fixtures.bxxTag)
    }

    "should disallow inconsistent tags" in {
      assertThrows[IllegalArgumentException] {
        Fixtures.invalidTagsDocumentation
      }
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
      schema =
        keys
          .foldLeft[Record[_]](emptyRecord)((schema, key) => schema.zip(field[Int](key)))
          .named("Resource")
      item = endpoint(
        get(path): Request[Unit],
        ok(jsonResponse(schema)): Response[Record[_]]
      )
      docs = openApi(Info("test", "0.0.0"))(item)
      json = ujson.read(OpenApi.stringEncoder.encode(docs))
    } {
      json("components")("schemas")("Resource")("properties").obj.toList shouldBe (keys.map(key =>
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

  val fooTag = Tag("foo")
  val barTag = Tag("bar").withDescription(Some("This is a bar."))
  val bazTag =
    Tag("baz").withExternalDocs(Some(ExternalDocumentationObject("my@url.com")))
  val bxxTag = Tag("bxx").withExternalDocs(
    Some(
      ExternalDocumentationObject("my@url.com").withDescription(
        Some("my@url.com contains the official documentation.")
      )
    )
  )

  val foo = endpoint(
    get(path / "foo"),
    ok(emptyResponse, Some("Foo response")),
    docs = EndpointDocs().withTags(List(fooTag)).withOperationId(Some("foo"))
  )

  val bar = endpoint(
    post(path / "foo", emptyRequest),
    ok(emptyResponse, Some("Bar response")),
    docs = EndpointDocs().withTags(List(barTag, bxxTag))
  )

  val baz = endpoint(
    get(path / "baz" / segment[Int]("quux")),
    ok(emptyResponse, Some("Baz response")),
    docs = EndpointDocs().withTags(List(bazTag, bxxTag))
  )

  val textRequestEndp = endpoint(
    post(path / "textRequestEndpoint", textRequest, docs = Some("Text Req")),
    ok(emptyResponse),
    docs = EndpointDocs().withDeprecated(true)
  )

  val emptySegmentNameEndp = endpoint(
    post(
      path / "emptySegmentNameEndp" / segment[Int]() / "x" / segment[String](),
      textRequest
    ),
    ok(emptyResponse)
  )

  val invalidTagsEndpoint = endpoint(
    get(path / "invalid"),
    ok(emptyResponse, Some("Some response")),
    docs = EndpointDocs().withTags(
      List(bazTag, bazTag.withDescription(Some("Inconsistent description")))
    )
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
    docs = EndpointDocs()
      .withSummary(Some("summary of endpoint"))
      .withDescription(Some("description of endpoint"))
  )
}

object Fixtures
    extends Fixtures
    with algebra.JsonSchemasFixtures
    with Endpoints
    with JsonEntitiesFromSchemas
    with ChunkedEntities {

  // throws an exception, thus a def instead of a val
  def invalidTagsDocumentation =
    openApi(Info("Test API", "1.0.0"))(invalidTagsEndpoint)

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
