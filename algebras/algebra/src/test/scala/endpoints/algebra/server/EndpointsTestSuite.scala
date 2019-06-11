package endpoints.algebra.server

import java.util.UUID

trait EndpointsTestSuite[T <: endpoints.algebra.Endpoints] extends ServerTestBase[T] {

  def urlsTestSuite() = {
    import serverApi.{ segment => s, _}
    import DecodedUrl._

    "paths" should {

      "static" in {
        decodeUrl(path)                 ("/")        shouldEqual Matched(())
        decodeUrl(path)                 ("/foo")     shouldEqual NotMatched
        decodeUrl(path / "foo")         ("/foo")     shouldEqual Matched(())
        decodeUrl(path / "foo")         ("/")        shouldEqual NotMatched
        decodeUrl(path / "foo" / "bar") ("/foo/bar") shouldEqual Matched(())
        decodeUrl(path / "foo" / "bar") ("/foo")     shouldEqual NotMatched
      }

      "decode segments" in {
        decodeUrl(path / s[Int]())         ("/42")        shouldEqual Matched(42)
        decodeUrl(path / s[Long]())        ("/42")        shouldEqual Matched(42L)
        decodeUrl(path / s[Double]())      ("/42.0")      shouldEqual Matched(42.0)
        decodeUrl(path / s[Int]())         ("/")          shouldEqual NotMatched
        decodeUrl(path / s[Int]())         ("/42/bar")    shouldEqual NotMatched
        decodeUrl(path / s[Int]())         ("/foo")       shouldEqual Malformed
        decodeUrl(path / s[String]())      ("/foo%20bar") shouldEqual Matched("foo bar")
        decodeUrl(path / s[String]())      ("/foo/bar")   shouldEqual NotMatched
        decodeUrl(path / s[Int]() / "baz") ("/42/baz")    shouldEqual Matched(42)
        decodeUrl(path / s[Int]() / "baz") ("/foo/baz")   shouldEqual Malformed
        decodeUrl(path / s[Int]() / "baz") ("/42")        shouldEqual NotMatched
//        decodeUrl(path / s[Int]() / "baz") ("/foo")       shouldEqual NotMatched
        decodeUrl(path / "foo" / remainingSegments()) ("/foo/bar%2Fbaz/quux") shouldEqual Matched("bar%2Fbaz/quux")
        decodeUrl(path / "foo" / remainingSegments()) ("/foo")                shouldEqual NotMatched
      }

      "transformed" in {
        val itemId = s[String]("itemId").xmapPartial { rawId =>
          val sep = rawId.indexOf("-")
          if (sep == -1) None else {
            val (id, name) = rawId.splitAt(sep)
            Some(Item(name.drop(1), id))
          }
        }(item => s"${item.id}-${item.name}")
        decodeUrl(path / itemId)("/42-programming-in-scala") shouldEqual Matched(Item("programming-in-scala", "42"))
        decodeUrl(path / itemId)("/foo")                     shouldEqual Malformed

        val file = s[String]("file").xmap(new java.io.File(_))(_.getPath)
        decodeUrl(path / "assets" / file)("/assets/favicon.png") shouldEqual Matched(new java.io.File("favicon.png"))
      }

    }

    "decode query strings" should {

      "primitives" in {
        decodeUrl(path / "foo" /? qs[Int]("n"))     ("/foo?n=42")    shouldEqual Matched(42)
        decodeUrl(path / "foo" /? qs[Long]("n"))    ("/foo?n=42")    shouldEqual Matched(42L)
        decodeUrl(path / "foo" /? qs[String]("s"))  ("/foo?s=bar")   shouldEqual Matched("bar")
        decodeUrl(path / "foo" /? qs[Boolean]("b")) ("/foo?b=true")  shouldEqual Matched(true)
        decodeUrl(path / "foo" /? qs[Boolean]("b")) ("/foo?b=false") shouldEqual Matched(false)
        decodeUrl(path / "foo" /? qs[Int]("n"))     ("/foo")         shouldEqual Malformed
        decodeUrl(path / "foo" /? qs[Int]("n"))     ("/foo?n=bar")   shouldEqual Malformed
      }

      "optional" in {
        val url = path /? qs[Option[Int]]("n")
        decodeUrl(url)("/")       shouldEqual Matched(None)
        decodeUrl(url)("/?n=42")  shouldEqual Matched(Some(42))
        decodeUrl(url)("/?n=bar") shouldEqual Malformed
      }

      "list" in {
        val url = path /? qs[List[Int]]("xs")
        decodeUrl(url)("/")             shouldEqual Matched(Nil)
        decodeUrl(url)("/?xs=1&xs=2")   shouldEqual Matched(1 :: 2 :: Nil)
        decodeUrl(url)("/?xs=1&xs=two") shouldEqual Malformed
      }

      "transformed" in {
        implicit val pageQueryString: QueryStringParam[Page] =
          intQueryString.xmap(Page)(_.number)
        val url = path /? qs[Page]("page")
        decodeUrl(url)("/?page=42")  shouldEqual Matched(Page(42))
        decodeUrl(url)("/?page=foo") shouldEqual Malformed
      }

    }

    "urls" should {

      "transformed" in {
        val paginatedUrl =
          (path /? (qs[Int]("from") & qs[Int]("limit")))
            .xmap(Page2.tupled)(p => (p.from, p.limit))
        decodeUrl(paginatedUrl)("/?from=1&limit=10")   shouldEqual Matched(Page2(1, 10))
        decodeUrl(paginatedUrl)("/?from=one&limit=10") shouldEqual Malformed
      }

    }

    "xmap query strings" should {

      "xmap urls of locations" in {
        

        val locationQueryString =
          (qs[Double]("lon") & qs[Double]("lat"))
            .xmap[Location]({ case (lon, lat) => Location(lon, lat) })(location => (location.longitude, location.latitude))
        val locationUrl = path /? locationQueryString
        
        decodeUrl(locationUrl)("/?lon=12.0&lat=32.9") shouldEqual Matched(Location(12.0, 32.9))
        decodeUrl(locationUrl)("/?lon=-12.0&lat=32.9") shouldEqual Matched(Location(-12.0, 32.9))
        decodeUrl(locationUrl)(s"/?lon=${Math.PI}&lat=-32.9") shouldEqual Matched(Location(Math.PI, -32.9))
        decodeUrl(locationUrl)("/?lon=12&lat=32") shouldEqual Matched(Location(12, 32))
        decodeUrl(locationUrl)("/?lat=32.0&lon=12.0") shouldEqual Matched(Location(12.0, 32.0))

        decodeUrl(locationUrl)("/?lon=12,0&lat=32.0") shouldEqual Malformed
        decodeUrl(locationUrl)("/?lon=a&lat=32") shouldEqual Malformed
        decodeUrl(locationUrl)("/?let=12.0&lat=32.0") shouldEqual Malformed
        decodeUrl(locationUrl)("/?lon=12.0") shouldEqual Malformed
      }

      "xmapPartial urls of blogids" in {
        val blogIdQueryString: QueryString[BlogId] =
          (qs[Option[UUID]]("uuid") & qs[Option[String]]("slug"))
            .xmapPartial {
              case (maybeUuid, maybeSlug) =>
                maybeUuid.map[BlogId](BlogUuid).orElse(maybeSlug.map[BlogId](BlogSlug))
            } {
              case BlogUuid(uuid) => (Some(uuid), None)
              case BlogSlug(slug) => (None, Some(slug))
            }
        
        val testUUID: String = "f4b9defa-1ad8-453f-9a06-2683b8564b8d"
        val testMalformedUUID1: String = "f4b9defa-1ad8-453f-9a06268d"
        val testMalformedUUID2: String = "f4b9defa-1ad8-453f-9o06-2683b8564b8d"
        val testSlug: String = "test-slug"

        decodeUrl(path /? blogIdQueryString)(s"/?uuid=$testUUID") shouldEqual Matched(BlogUuid(UUID.fromString(testUUID)))
        decodeUrl(path /? blogIdQueryString)(s"/?slug=$testSlug") shouldEqual Matched(BlogSlug(testSlug))
        decodeUrl(path /? blogIdQueryString)(s"/?uuid=$testUUID&slug=$testSlug") shouldEqual Matched(BlogUuid(UUID.fromString(testUUID)))
        decodeUrl(path /? blogIdQueryString)(s"/?slug=$testSlug&uuid=$testUUID") shouldEqual Matched(BlogUuid(UUID.fromString(testUUID)))
        decodeUrl(path /? blogIdQueryString)(s"/?slug=") shouldEqual Matched(BlogSlug(""))

        decodeUrl(path /? blogIdQueryString)(s"/?uuid=$testMalformedUUID1") shouldEqual Malformed
        decodeUrl(path /? blogIdQueryString)(s"/?uuid=$testMalformedUUID2") shouldEqual Malformed
      }

    }

  }

}

case class Item(name: String, id: String)
case class Page(number: Int)
case class Page2(from: Int, limit: Int)
case class Location(longitude: Double, latitude: Double)
sealed trait BlogId
case class BlogUuid(uuid: UUID) extends BlogId
case class BlogSlug(slug: String) extends BlogId
