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
        decodeUrl(path / s[Int]())         ("/")          shouldEqual NotMatched
        decodeUrl(path / s[Int]())         ("/42/bar")    shouldEqual NotMatched
        decodeUrl(path / s[Int]())         ("/foo")       shouldEqual Malformed
        decodeUrl(path / s[String]())      ("/foo%20bar") shouldEqual Matched("foo bar")
        decodeUrl(path / s[String]())      ("/foo/bar")   shouldEqual NotMatched
        decodeUrl(path / s[Int]() / "baz") ("/42/baz")    shouldEqual Matched(42)
        decodeUrl(path / s[Int]() / "baz") ("/foo/baz")   shouldEqual Malformed
        decodeUrl(path / s[Int]() / "baz") ("/42")        shouldEqual NotMatched
//        decodeUrl(path / s[Int]() / "baz") ("/foo")       shouldEqual NotMatched
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

    }

  }

}
