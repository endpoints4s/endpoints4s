# Assets

This algebra provides vocabulary to define endpoints serving static assets.

@coordinates[algebra]

@scaladoc[API documentation](endpoints4s.algebra.Assets)

The module enriches the `Endpoints` algebra with new constructors for endpoints and
path segments. It also introduces the concepts of `AssetRequest`, `AssetResponse`
and `AssetPath`. The typical usage looks like the following:

@@snip [AssetsDocs.scala](/algebras/algebra/src/test/scala/endpoints4s/algebra/AssetsDocs.scala) { #assets-endpoint }

The `assetsSegments` method defines a path containing (possibly) multiple segments.

The concrete instantiation of the `AssetRequest` and `AssetResponse` types is left to
interpreters. Typically, `AssetResponse` is mapped to binary data. Interpreters also
have to provide a constructor for `AssetRequest`, so that the endpoint can be called.
Typically, such constructors take the path of the asset as a `String` parameter.

Server interpreters are encouraged to leverage caching HTTP headers such as `ETag`
or `Cache-Control`, and gzip content encoding. Incidentally, the algebra provides
an abstract `digest: Map[String, String]` member to be overridden by users with
digests uniquely identifying the assets:

@@snip [AssetsDocs.scala](/algebras/algebra/src/test/scala/endpoints4s/algebra/AssetsDocs.scala) { #digests }

The content of the digests can be included to the asset segments so that servers
know that the requested version of the asset matches the one it uses, enabling servers
to indefinitely cache the asset.
