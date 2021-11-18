package endpoints4s.fetch.future

import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

import endpoints4s.algebra
import endpoints4s.algebra.Address
import endpoints4s.algebra.User
import endpoints4s.fetch.BasicAuthentication
import endpoints4s.fetch.EndpointsSettings
import endpoints4s.fetch.JsonEntitiesFromCodecs

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext

class TestClient(val settings: EndpointsSettings)(implicit val ec: ExecutionContext)
    extends Endpoints
    with BasicAuthentication
    with algebra.EndpointsTestApi
    with algebra.BasicAuthenticationTestApi
    with BasicAuthenticationTestApi
    with algebra.TextEntitiesTestApi
    with algebra.JsonFromCodecTestApi
    with algebra.SumTypedEntitiesTestApi
    with algebra.circe.JsonFromCirceCodecTestApi
    with JsonEntitiesFromCodecs
    with algebra.circe.JsonEntitiesFromCodecs {

  val errorEndpoint = endpoint(
    get(
      path / "error" / "user" / segment[String]() / "description" /? (qs[String]("name") & qs[
        Int
      ]("age"))
    ),
    ok(textResponse)
  )

  val emptyResponseErrorEndpoint = endpoint(
    get(
      path / "error" / "user" / segment[String]() / "description" /? (qs[String]("name") & qs[
        Int
      ]("age"))
    ),
    ok(emptyResponse)
  )

  val detailedErrorEndpoint = endpoint(
    get(
      path / "detailed" / "error" / "user" / segment[String]() / "description" /? (qs[String](
        "name"
      ) & qs[
        Int
      ]("age"))
    ),
    ok(textResponse)
  )

  val emptyResponseDetailedErrorEndpoint = endpoint(
    get(
      path / "detailed" / "error" / "user" / segment[String]() / "description" /? (qs[String](
        "name"
      ) & qs[
        Int
      ]("age"))
    ),
    ok(emptyResponse)
  )

  val notFoundOptionalEndpoint: Endpoint[Unit, Option[String]] = endpoint(
    get(path / "not" / "found" / "users" / "1"),
    wheneverFound(ok(textResponse))
  )

  val someOptionalResponseHeader = endpoint(
    get(path / "optional-response-header" / "some"),
    ok(textResponse, headers = optResponseHeader("A"))
  )

  val noneOptionalResponseHeader = endpoint(
    get(path / "optional-response-header" / "none"),
    ok(textResponse, headers = optResponseHeader("A"))
  )
}

class FetchClientEndpointsTest
    extends ClientTestBase[TestClient]
    with BasicAuthTestSuite[TestClient] {

  implicit override def executionContext = JSExecutionContext.queue

  val client: TestClient = new TestClient(
    EndpointsSettings().withHost(Some("http://localhost:8080"))
  )

  def call[Req, Resp](
      endpoint: client.Endpoint[Req, Resp],
      args: Req
  ): Future[Resp] = endpoint(args)

  "Client interpreted" should {

    "return server response for UUID" in {
      val uuid = UUID.fromString("f3ac9be0-6339-4650-afb6-7305ece8edce")
      val response = "wiremockeResponse"

      for {
        _ <- call(client.UUIDEndpoint, (uuid, "name1", 18))
          .map(_ shouldEqual response)
        _ <- call(client.emptyResponseUUIDEndpoint, (uuid, "name1", 18))
          .map(_ shouldEqual (()))
      } yield succeed
    }

    "return server response" in {
      val response = "wiremockeResponse"

      for {
        _ <- call(client.smokeEndpoint, ("userId", "name1", 18))
          .map(_ shouldEqual response)
        _ <- call(client.emptyResponseSmokeEndpoint, ("userId", "name1", 18))
          .map(_ shouldEqual (()))
      } yield succeed
    }

    "return correct url with optional UUID parameter" in {
      val uuid = UUID.fromString("1bdae951-63ee-46b9-8ff0-4976acb8d48e")
      val response = "wiremockeResponse"

      for {
        _ <- call(client.optUUIDQsEndpoint, ("userId", uuid, None))
          .map(_ shouldEqual response)
        _ <- call(client.optUUIDQsEndpoint, ("userId", uuid, Some(18)))
          .map(_ shouldEqual response)
      } yield succeed
    }

    "return correct url with optional parameter" in {
      val response = "wiremockeResponse"

      for {
        _ <- call(client.optQsEndpoint, ("userId", "name1", None))
          .map(_ shouldEqual response)
        _ <- call(client.optQsEndpoint, ("userId", "name1", Some(18)))
          .map(_ shouldEqual response)
      } yield succeed
    }

    "throw exception when 5xx is returned from server" in {
      for {
        _ <- call(client.errorEndpoint, ("userId", "name1", 18)).failed
          .map(x => x.getMessage shouldBe "Unexpected response status: 501")
        _ <- call(client.emptyResponseErrorEndpoint, ("userId", "name1", 18)).failed
          .map(x => x.getMessage shouldBe "Unexpected response status: 501")
      } yield succeed
    }

    "throw exception with a detailed error message when 500 is returned from server" in {
      for {
        _ <- call(client.detailedErrorEndpoint, ("userId", "name1", 18)).failed
          .map(x => x.getMessage shouldBe "Unable to process your request")
        _ <- call(client.emptyResponseDetailedErrorEndpoint, ("userId", "name1", 18)).failed
          .map(x => x.getMessage shouldBe "Unable to process your request")
      } yield succeed
    }

    "properly handle joined headers" in {
      val response = UUID.fromString("29d15495-55ea-431e-bef3-392b05b14fef").toString

      call(client.joinedHeadersEndpoint, ("a", "b"))
        .map(_ shouldEqual response)
    }

    "properly handle xmaped headers" in {
      val response = UUID.fromString("f2ed5a13-9113-4717-9b21-65cd72a5540e").toString

      client
        .xmapHeadersEndpoint(11)
        .map(_ shouldEqual response)
    }

    "properly handle xmaped url" in {
      val response = UUID.fromString("f4e4ccbf-710a-4b38-bf8b-a9eb0a92382c").toString

      client
        .xmapUrlEndpoint("11")
        .map(_ shouldEqual response)
    }

    "properly handle xmaped request entites" in {
      val response = UUID.fromString("dbb2297e-ae8c-4413-aab3-978833794c79").toString
      val dateString = "2018-04-14"
      val date = LocalDate.parse(dateString)

      client
        .xmapReqBodyEndpoint(date)
        .map(_ shouldEqual response)
    }

    "in case of optional response" should {

      "return Some when response code is 2xx" in {
        val response = "wiremockeResponse"

        client
          .optionalEndpoint(())
          .map(_ shouldEqual Some(response))
      }

      "return None if server returned 404" in {
        client
          .notFoundOptionalEndpoint(())
          .map(_ shouldEqual None)
      }
    }

    "return correct url with trailing slash" in {
      client
        .trailingSlashEndpoint(())
        .map(_ shouldEqual (()))
    }

    "Decode response headers" in {
      val response = "foo"
      val etag = UUID.fromString("d88b0456-67cb-40e5-8f0a-7664f3e93348").toString
      val lastModified = DateTimeFormatter.RFC_1123_DATE_TIME
        .format(ZonedDateTime.parse("2021-01-01T12:30Z"))

      call(client.versionedResource, ())
        .map { case (entity, cache) =>
          assert(entity == response)
          assert(
            cache.etag.startsWith(etag.dropRight(1))
          ) // Some http client add a “--gzip” suffix
          assert(cache.lastModified == lastModified)
        }
    }

    "Decode optional response headers" in {
      call(client.someOptionalResponseHeader, ())
        .map { case (entity, header) =>
          (entity, header) shouldEqual (("foo", Some("a")))
        }
    }

    "Decode missing optional response headers" in {
      call(client.noneOptionalResponseHeader, ())
        .map { case (entity, header) =>
          (entity, header) shouldEqual (("foo", None))
        }
    }
  }

  "Client interpreter" should {

    "encode JSON requests and decode JSON responses" in {

      val user = User("name2", 19)
      val address = Address("avenue1", "NY")

      client
        .jsonCodecEndpoint(user)
        .map(_ shouldEqual address)
    }
  }

  "TextEntities" should {

    "produce `text/plain` requests with an explicit encoding" in {
      val utf8String = "Oekraïene"

      client
        .textRequestEndpointTest(utf8String)
        .map(_ shouldEqual utf8String)
    }
  }

  "Client interpreter" should {

    "handle the sum-typed request entities" in {
      val user = algebra.User("name2", 19)
      val name = "name3"

      for {
        _ <- call(client.sumTypedEndpoint2, Left(user))
          .map(_.shouldEqual(()))
        _ <- call(client.sumTypedEndpoint2, Right(name))
          .map(_.shouldEqual(()))
      } yield succeed
    }
  }
}
