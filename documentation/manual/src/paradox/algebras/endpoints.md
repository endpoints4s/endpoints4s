# `Endpoints`

This algebra, at the top of the hierarchy, provides the base vocabulary to
describe HTTP endpoints, requests and responses.

@@@vars
~~~ scala
"org.julienrf" %% "endpoints-algebra" % "$version$"
~~~
@@@

@scaladoc[API documentation](endpoints.algebra.Endpoints)

## Endpoint

The algebra introduces the concept of `Endpoint[A, B]`: an HTTP endpoint
whose request carries an information of type `A` and whose response carries
an information of type `B`. For instance, an endpoint of type
`Endpoint[Long, Option[User]]` has a request containing a `Long` (for instance,
a user id), and a response optionally containing a `User`.

You can define an endpoint by using the `endpoint` constructor:

@@snip [EndpointsDocs.scala](/algebras/algebra/src/test/scala/endpoints/algebra/EndpointsDocs.scala) { #construction }

The `endpoint` constructor takes two parameters, the request description
(of type `Request[A]`) and the response description (of type `Response[B]`),
which are documented in the following sections.

It also takes optional parameters carrying documentation information:

@@snip [EndpointsDocs.scala](/algebras/algebra/src/test/scala/endpoints/algebra/EndpointsDocs.scala) { #with-docs }

## Request

The `Request[A]` type models an HTTP request carrying
some information of type `A`. For instance, a `Request[Long]` value
is a request containing a `Long` value.

A request is defined in terms of an HTTP verb, an URL, an entity and headers:

@@snip [EndpointsDocs.scala](/algebras/algebra/src/test/scala/endpoints/algebra/EndpointsDocs.scala) { #request-construction }

For convenience, `get`, `post`, `put` and `delete` methods are provided:

@@snip [EndpointsDocs.scala](/algebras/algebra/src/test/scala/endpoints/algebra/EndpointsDocs.scala) { #convenient-get }

The next sections document how to describe URLs, request headers and request entities.

### URL

The `Url[A]` type models an URL carrying some information of type `A`. For
instance, an `Url[Long]` value is an URL containing a `Long` value.

An URL is defined by a path and a query string. Here are some self-explanatory
examples of URLs:

@@snip [EndpointsDocs.scala](/algebras/algebra/src/test/scala/endpoints/algebra/EndpointsDocs.scala) { #urls }

The examples above show that basic types (e.g., `Int`, `String`, etc.) are supported out of the box as query
and path parameters. A user-defined type `T` can be supported either by

- defining implicit instances of `Segment[T]` (for path parameters) or `QueryStringParam[T]` (for query string
  parameters),
- transforming or refining already supported types by using `xmap` or `xmapPartial` (see next section).

Path segments and query string parameters can take additional parameters containing documentation:

@@snip [EndpointsDocs.scala](/algebras/algebra/src/test/scala/endpoints/algebra/EndpointsDocs.scala) { #urls-with-docs }

#### Transforming and Refining URL Constituents

All the data types involved in a URL description (`Path[A]`, `Segment[A]`, `QueryString[A]`, etc.) have an
`xmap` and an `xmapPartial` operations, for transforming or refining their carried type.

For instance, consider the following user-defined `Location` type, containing a `longitude` and a `latitude`:

@@snip [EndpointsTestSuite.scala](/algebras/algebra/src/test/scala/endpoints/algebra/client/EndpointsTestSuite.scala) { #location-type }

The `QueryString[Location]` type means “a query string that carries a `Location`”. We can define a value of
type `QueryString[Location]` by *transforming* a query string that carries the longitude and latitude parameters
as follows:

@@snip [EndpointsTestSuite.scala](/algebras/algebra/src/test/scala/endpoints/algebra/client/EndpointsTestSuite.scala) { #xmap }

The `xmap` operation requires the source type and the target type to be equivalent (in the above case, the
source type is `(Double, Double)` and the target type is `Location`).

In case the target type is smaller than the source type, you can use the `xmapPartial` operation, which *refines*
the carried type. As an example, here is how you can define a `Segment[LocalDate]`:

@@snip [EndpointsDocs.scala](/algebras/algebra/src/test/scala/endpoints/algebra/EndpointsDocs.scala) { #xmap-partial }

The first function passed to the `xmapPartial` operation returns a
@scaladoc[`Validated[LocalDate]`](endpoints.Validated) value. Returning an
`Invalid` value means that there is no representation of the source type in the target type.

### Request Headers

The type `RequestHeaders[A]` models request headers carrying some information of type `A`. For
instance, a value of type `RequestHeaders[Credentials]` describes request headers containing
credentials.

Please refer to the @scaladoc[API documentation](endpoints.algebra.Endpoints#RequestHeaders[A]) for
details about constructors and operations for the type `RequestHeaders`.

### Request Entity

The type `RequestEntity[A]` models a request entity carrying some information of type `A`. For
instance, a value of type `RequestEntity[Command]` describes a request entity containing a
command.

The `Endpoints` algebra provides a few @scaladoc[`RequestEntity` constructors and operations](endpoints.algebra.Endpoints#RequestEntity[A]),
which can be extended to support more content types. For instance, the
@ref[JsonEntities](json-entities.md) algebra adds support for requests with JSON entities.

## Response

The `Response[A]` type models an HTTP response carrying some information of type `A`.
For instance, a `Response[User]` value describes an HTTP response containing a user:
client interpreters decode a `User` from the response entity, server interpreters
encode a `User` as a response entity, and documentation interpreters render the
serialization schema of a `User`.

### Constructing Responses

A response is defined in terms of a status, headers and an entity. Here is an example
of a simple OK response with no entity and no headers:

@@snip [EndpointsDocs.scala](/algebras/algebra/src/test/scala/endpoints/algebra/EndpointsDocs.scala) { #response }

There is a more general response constructor taking the status as parameter:

@@snip [EndpointsDocs.scala](/algebras/algebra/src/test/scala/endpoints/algebra/EndpointsDocs.scala) { #general-response }

Additional documentation about the response can be passed as an extra parameter:

@@snip [EndpointsDocs.scala](/algebras/algebra/src/test/scala/endpoints/algebra/EndpointsDocs.scala) { #documented-response }

### Response Headers

The type `ResponseHeaders[A]` models response headers carrying some information of type `A`. For
instance, a value of type `ResponseHeaders[Origin]` describes response headers containing an
origin (e.g., an `Access-Control-Allow-Origin` header).

Refer to the @scaladoc[API documentation](endpoints.algebra.Endpoints#ResponseHeaders[A]) for
details about constructors and operations for the type `ResponseHeaders`.

### Response Entity

The type `ResponseEntity[A]` models a response entity carrying some information of type `A`. For
instance, a value of type `ResponseEntity[Event]` describes a response entity containing an event.

The `Endpoints` algebra provides a few @scaladoc[`ResponseEntity` constructors and operations](endpoints.algebra.Endpoints#ResponseEntity[A]),
which can be extended to support more content-types. For instance, the
@ref[JsonEntities](json-entities.md) algebra adds support for responses with JSON entities.

### Transforming Responses

Responses have methods provided by the
@scaladoc[`ResponseSyntax`](endpoints.algebra.Responses$ResponseSyntax)
and the
@scaladoc[`InvariantFunctorSyntax`](endpoints.InvariantFunctorSyntax$InvariantFunctorSyntax)
implicit classes, whose usage is illustrated in the remaining of this section.

The `orNotFound` operation is useful to handle resources that may not be found: 

@@snip [JsonEntitiesDocs.scala](/algebras/algebra/src/test/scala/endpoints/algebra/JsonEntitiesDocs.scala) { #response-or-not-found }

In this example, servers can produce a Not Found (404) response by
returning `None`, and an OK (200) response containing a user by returning
a `Some[User]` value. Conversely, clients interpret a Not Found response as a `None`
value, and an OK response (with a valid user entity) as a `Some[User]` value.

More generally, you can describe an alternative between two possible responses
by using the `orElse` operation:

@@snip [JsonEntitiesDocs.scala](/algebras/algebra/src/test/scala/endpoints/algebra/JsonEntitiesDocs.scala) { #response-or-else }

In this example, servers can produce a Not Implemented (501) response by returning
`Left(())`, and an OK (200) response containing a user
by returning `Right(user)`. Conversely, clients interpret a Not Implemented response
as a `Left(())` value,
and an OK response (with a valid user entity) as a `Right(user)` value.

You can also transform the type produced by the alternative responses into
a more convenient type to work with, by using the `xmap` operation. For instance,
here is how to transform a `Response[Either[Unit, User]]` into a
`Response[Option[User]]`:

@@snip [JsonEntitiesDocs.scala](/algebras/algebra/src/test/scala/endpoints/algebra/JsonEntitiesDocs.scala) { #response-xmap }

### Error Responses

_endpoints_ server interpreters handle two kinds of errors:

- when the server is unable to decode an incoming request (because, for instance,
  a query parameter is missing, or the request entity has the wrong format).
  In this case it is a “client error” ;
- when the provided business logic throws an exception, or the server is
  unable to serialize the result into a proper HTTP response. In this case it is
  a “server error”.

By default, client errors are reported as an
@scaladoc[Invalid](endpoints.Invalid) value, serialized into
a Bad Request (400) response, as a JSON array containing string messages.
You can change the provided serialization format by overriding the
@scaladoc[clientErrorsResponseEntity](endpoints.algebra.BuiltInErrors#clientErrorsResponseEntity:BuiltInErrors.this.ResponseEntity[endpoints.Invalid])
operation.

Similarly, by default server errors are reported as a `Throwable` value,
serialized into an Internal Server Error (500) response, as a JSON array
containing string messages. You can change the provided serialization format
by overriding the
@scaladoc[serverErrorResponseEntity](endpoints.algebra.BuiltInErrors#serverErrorResponseEntity:BuiltInErrors.this.ResponseEntity[Throwable])
operation.

## Next Step

See how you can describe endpoints with @ref[JSON entities](json-entities.md).
