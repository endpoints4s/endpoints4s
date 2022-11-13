package endpoints4s.algebra.client

import java.util.UUID

import endpoints4s.algebra
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import endpoints4s.{Invalid, Valid}

trait UrlEncodingTestSuite[T <: algebra.client.ClientEndpointsTestApi]
    extends AnyWordSpecLike
    with Matchers {

  val stubServerPort = 8080

  val client: T

  def encodeUrl[A](url: client.Url[A])(a: A): String

  "encode query strings" should {
    import client._

    "primitives" in {
      encodeUrl(path / "foo" /? qs[Int]("n"))(42) shouldEqual "/foo?n=42"
      encodeUrl(path / "foo" /? qs[Long]("n"))(42L) shouldEqual "/foo?n=42"
      encodeUrl(path / "foo" /? qs[String]("s"))("bar") shouldEqual "/foo?s=bar"
      encodeUrl(path / "foo" /? qs[Boolean]("b"))(true) shouldEqual "/foo?b=true"
      encodeUrl(path / "foo" /? qs[Double]("x"))(1.0) shouldEqual (
        VM.current match {
          case VM.JS =>
            "/foo?x=1"
          case VM.JVM =>
            "/foo?x=1.0"
        }
      )
      encodeUrl(path / "foo" /? qs[UUID]("id"))(
        UUID.fromString("f4b9defa-1ad8-453f-9a06-2683b8564b8d")
      ) shouldEqual "/foo?id=f4b9defa-1ad8-453f-9a06-2683b8564b8d"
    }

    "escaping" in {
      // Clients may encode space as '+' or '%20' (see https://stackoverflow.com/questions/2678551/when-should-space-be-encoded-to-plus-or-20)
      val encodedSpace = encodeUrl(path /? qs[String]("q"))("foo bar")
      assert(encodedSpace == "?q=foo%20bar" || encodedSpace == "?q=foo+bar")
      // Clients may or may not encode slashes (see https://datatracker.ietf.org/doc/html/rfc3986#section-2.4)
      val encodedSlash = encodeUrl(path /? qs[String]("q"))("foo/bar")
      assert(encodedSlash == "?q=foo/bar" || encodedSlash == "?q=foo%2Fbar")
    }

    "multiple parameters" in {
      encodeUrl(path /? (qs[Int]("x") & qs[Int]("y")))((0, 1)) shouldEqual "?x=0&y=1"
    }

    "optional parameters" in {
      encodeUrl(path /? qs[Option[Int]]("n"))(Some(42)) shouldEqual "?n=42"
      encodeUrl(path /? qs[Option[Int]]("n"))(None) shouldEqual ""
      encodeUrl(path /? (qs[Option[Int]]("n") & qs[Int]("v")))((None, 42)) shouldEqual "?v=42"
      encodeUrl(path /? (qs[Option[Int]]("n") & qs[Int]("v")))(
        (Some(0), 42)
      ) shouldEqual "?n=0&v=42"
    }

    "optional parameters with default value" in {
      encodeUrl(path /? optQsWithDefault[Int]("n", 42))(Some(42)) shouldEqual "?n=42"
      encodeUrl(path /? optQsWithDefault[Int]("n", 42))(Some(43)) shouldEqual "?n=43"
      encodeUrl(path /? optQsWithDefault[Int]("n", 42))(None) shouldEqual ""
      encodeUrl(path /? (optQsWithDefault[Int]("n", 0) & qs[Int]("v")))(
        (None, 42)
      ) shouldEqual "?v=42"
      encodeUrl(path /? (optQsWithDefault[Int]("n", 0) & qs[Int]("v")))(
        (Some(0), 42)
      ) shouldEqual "?n=0&v=42"
      encodeUrl(path /? (optQsWithDefault[Int]("n", 0) & qs[Int]("v")))(
        (Some(1), 42)
      ) shouldEqual "?n=1&v=42"
    }

    "list parameters" in {
      encodeUrl(path /? qs[List[Int]]("ids"))(1 :: 2 :: Nil) shouldEqual "?ids=1&ids=2"
      encodeUrl(path /? qs[List[Int]]("ids"))(Nil) shouldEqual ""
      encodeUrl(path /? (qs[List[Int]]("ids") & qs[Option[Int]]("x")))(
        (Nil, None)
      ) shouldEqual ""
      encodeUrl(path /? (qs[List[Int]]("ids") & qs[Option[Int]]("x")))(
        (Nil, Some(0))
      ) shouldEqual "?x=0"
      encodeUrl(path /? (qs[List[Int]]("ids") & qs[Option[Int]]("x")))(
        (1 :: Nil, None)
      ) shouldEqual "?ids=1"
    }

  }

  "encode path segments" in {
    import client._
    encodeUrl(path / "foo" / segment[String]())("bar/baz") shouldEqual "/foo/bar%2Fbaz"
    encodeUrl(path / segment[String]())("bar/baz") shouldEqual "/bar%2Fbaz"
    encodeUrl(path / segment[String]())("bar baz") shouldEqual "/bar%20baz"
    encodeUrl(path / segment[String]() / "baz")("bar") shouldEqual "/bar/baz"
    encodeUrl(path / segment[Int]())(42) shouldEqual "/42"
    encodeUrl(path / segment[Long]())(42L) shouldEqual "/42"
    encodeUrl(path / segment[Double]())(42.0) shouldEqual (
      VM.current match {
        case endpoints4s.algebra.client.VM.JS =>
          "/42"
        case endpoints4s.algebra.client.VM.JVM =>
          "/42.0"
      }
    )
    encodeUrl(path / "foo" / remainingSegments())(
      "bar%2Fbaz/quux"
    ) shouldEqual "/foo/bar%2Fbaz/quux"

    val evenNumber = segment[Int]().xmapPartial {
      case x if x % 2 == 0 => Valid(x)
      case x               => Invalid(s"Invalid odd value '$x'")
    }(identity)
    encodeUrl(path / evenNumber)(42) shouldEqual "/42"
  }

  "xmap query string" should {
    import client._

    "xmap locations" in {
      //#location-type
      case class Location(longitude: Double, latitude: Double)
      //#location-type

      //#xmap
      val locationQueryString: QueryString[Location] =
        (qs[Double]("lon") & qs[Double]("lat")).xmap { case (lon, lat) =>
          Location(lon, lat)
        } { location => (location.longitude, location.latitude) }
      //#xmap

      encodeUrl(path /? locationQueryString)(
        Location(12.0, 32.9)
      ) shouldEqual (
        VM.current match {
          case endpoints4s.algebra.client.VM.JS =>
            "?lon=12&lat=32.9"
          case endpoints4s.algebra.client.VM.JVM =>
            "?lon=12.0&lat=32.9"
        }
      )
      encodeUrl(path /? locationQueryString)(
        Location(-12.0, 32.9)
      ) shouldEqual (
        VM.current match {
          case endpoints4s.algebra.client.VM.JS =>
            "?lon=-12&lat=32.9"
          case endpoints4s.algebra.client.VM.JVM =>
            "?lon=-12.0&lat=32.9"
        }
      )
      encodeUrl(path /? locationQueryString)(
        Location(Math.PI, -32.9)
      ) shouldEqual s"?lon=${Math.PI}&lat=-32.9"
    }

    "xmapPartial blogids" in {
      sealed trait BlogId
      case class BlogUuid(uuid: UUID) extends BlogId
      case class BlogSlug(slug: String) extends BlogId

      val blogIdQueryString: QueryString[BlogId] =
        (qs[Option[UUID]]("uuid") & qs[Option[String]]("slug"))
          .xmapPartial[BlogId] {
            case (Some(uuid), _)    => Valid(BlogUuid(uuid))
            case (None, Some(slug)) => Valid(BlogSlug(slug))
            case (None, None) =>
              Invalid("Missing either query parameter 'uuid' or 'slug'")
          } {
            case BlogUuid(uuid) => (Some(uuid), None)
            case BlogSlug(slug) => (None, Some(slug))
          }

      val testUUID: UUID = UUID.randomUUID()
      val testSlug: String = "test-slug"

      encodeUrl(path /? blogIdQueryString)(BlogUuid(testUUID)) shouldEqual s"?uuid=$testUUID"
      encodeUrl(path /? blogIdQueryString)(BlogSlug(testSlug)) shouldEqual s"?slug=$testSlug"
    }

  }
}
