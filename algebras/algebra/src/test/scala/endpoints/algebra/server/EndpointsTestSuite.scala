package endpoints.algebra.server

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

        val file = s[String]("file").xmap[java.io.File](new java.io.File(_), _.getPath)
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
          intQueryString.xmap[Page](Page, _.number)
        val url = path /? qs[Page]("page")
        decodeUrl(url)("/?page=42")  shouldEqual Matched(Page(42))
        decodeUrl(url)("/?page=foo") shouldEqual Malformed
      }

    }

    "urls" should {

      "transformed" in {
        val paginatedUrl =
          (path /? (qs[Int]("from") & qs[Int]("limit")))
            .xmap[Page2](Page2.tupled, p => (p.from, p.limit))
        decodeUrl(paginatedUrl)("/?from=1&limit=10")   shouldEqual Matched(Page2(1, 10))
        decodeUrl(paginatedUrl)("/?from=one&limit=10") shouldEqual Malformed
      }

    }

  }

}

case class Item(name: String, id: String)
case class Page(number: Int)
case class Page2(from: Int, limit: Int)
