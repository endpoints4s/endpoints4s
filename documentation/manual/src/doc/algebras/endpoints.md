# `Endpoints`

This algebra, at the top of the hierarchy, provides the base vocabulary to
describe HTTP endpoints, requests and responses.

~~~ scala expandVars=true
"org.julienrf" %% "endpoints-algebra" % "{{version}}"
~~~

[API documentation](unchecked:/api/endpoints/algebra/Endpoints.html)

## Endpoint

The algebra introduces the concept of `Endpoint[A, B]`: an HTTP endpoint
whose request carries an information of type `A` and whose response carries
an information of type `B`. For instance, an endpoint of type
`Endpoint[Long, Option[User]]` has a request containing a `Long` (for instance,
a user id), and a response optionally containing a `User`.

You can define an endpoint by using the `endpoint` constructor:

~~~ scala src=../../../../../algebras/algebra/src/test/scala/endpoints/algebra/EndpointsDocs.scala#construction
~~~

The `endpoint` constructor takes two parameters, the request description
(of type `Request[A]`) and the response description (of type `Response[B]`),
which are documented in the following sections.

It also takes optional parameters carrying documentation information:

~~~ scala src=../../../../../algebras/algebra/src/test/scala/endpoints/algebra/EndpointsDocs.scala#with-docs
~~~

## Request

The `Request[A]` type models an HTTP request carrying
some information of type `A`. For instance, a `Request[Long]` value
is a request containing a `Long` value.

A request is defined in terms of an HTTP verb, an URL, an entity and headers:

~~~ scala src=../../../../../algebras/algebra/src/test/scala/endpoints/algebra/EndpointsDocs.scala#request-construction
~~~

For convenience, `get`, `post`, `put` and `delete` methods are provided:

~~~ scala src=../../../../../algebras/algebra/src/test/scala/endpoints/algebra/EndpointsDocs.scala#convenient-get
~~~

## URL

The `Url[A]` type models an URL carrying some information of type `A`. For
instance, an `Url[Long]` value is an URL containing a `Long` value.

An URL is defined by a path and a query string. Here are some self-explanatory
examples of URLs:

~~~ scala src=../../../../../algebras/algebra/src/test/scala/endpoints/algebra/EndpointsDocs.scala#urls
~~~

The examples above show that basic types (e.g., `Int`, `String`, etc.) are supported out of the box as query
and path parameters. A user-defined type `T` can be supported either by
- defining implicit instances of `Segment[T]` (for path parameters) or `QueryStringParam[T]` (for query string
  parameters),
- transforming or refining already supported types by using `xmap` or `xmapPartial` (see next section).

Path segments and query string parameters can take additional parameters containing documentation:

~~~ scala src=../../../../../algebras/algebra/src/test/scala/endpoints/algebra/EndpointsDocs.scala#urls-with-docs
~~~

### Transforming and Refining URL Constituents

All the data types involved in a URL description (`Path[A]`, `Segment[A]`, `QueryString[A]`, etc.) have an
`xmap` and an `xmapPartial` operations, for transforming or refining their carried type.

For instance, consider the following user-defined `Location` type, containing a `longitude` and a `latitude`:

~~~ scala src=../../../../../algebras/algebra/src/test/scala/endpoints/algebra/client/EndpointsTestSuite.scala#location-type
~~~

The `QueryString[Location]` type means “a query string that carries a `Location`”. We can define a value of
type `QueryString[Location]` by *transforming* a query string that carries the longitude and latitude parameters
as follows:

~~~ scala src=../../../../../algebras/algebra/src/test/scala/endpoints/algebra/client/EndpointsTestSuite.scala#xmap
~~~

The `xmap` operation requires the source type and the target type to be equivalent (in the above case, the
source type is `(Double, Double)` and the target type is `Location`).

In case the target type is smaller than the source type, you can use the `xmapPartial` operation, which *refines*
the carried type. As an example, here is how you can define a `Segment[LocalDate]`:

~~~ scala src=../../../../../algebras/algebra/src/test/scala/endpoints/algebra/EndpointsDocs.scala#xmap-partial
~~~

The first function passed to the `xmapPartial` operation returns an `Option[LocalDate]`. Returning `None` means
that there is no representation of the source type in the target type.

## Response

The `Response[A]` type models an HTTP response carrying some information of type `A`.
For instance, a `Response[User]` value is an HTTP response containing a user.

A response can be defined by using a constructor:

~~~ scala src=../../../../../algebras/algebra/src/test/scala/endpoints/algebra/EndpointsDocs.scala#response
~~~

Or by using a combinator:

~~~ scala src=../../../../../algebras/algebra/src/test/scala/endpoints/algebra/EndpointsDocs.scala#response-combinator
~~~
