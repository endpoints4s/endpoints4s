# `Endpoints`

This algebra, at the top of the hierarchy, provides the base vocabulary to
describe HTTP endpoints, requests and responses.

~~~ scala expandVars=true
"org.julienrf" %% "endpoints-algebra" % "{{version}}"
~~~

[API documentation](api:endpoints.algebra.Endpoints)

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

For convenience, a `get` method and a `post` method are provided:

~~~ scala src=../../../../../algebras/algebra/src/test/scala/endpoints/algebra/EndpointsDocs.scala#convenient-get
~~~

## URL

The `Url[A]` type models an URL carrying some information of type `A`. For
instance, an `Url[Long]` value is an URL containing a `Long` value.

An URL is defined by a path and a query string. Here are some self-explanatory
examples of URLs:

~~~ scala src=../../../../../algebras/algebra/src/test/scala/endpoints/algebra/EndpointsDocs.scala#urls
~~~

Path segments and query string parameters can take additional parameters containing documentation:

~~~ scala src=../../../../../algebras/algebra/src/test/scala/endpoints/algebra/EndpointsDocs.scala#urls-with-docs
~~~

## Response

The `Response[A]` type models an HTTP response carrying some information of type `A`.
For instance, a `Response[User]` value is an HTTP response containing a user.

A responses can be defined by using a constructor:

~~~ scala src=../../../../../algebras/algebra/src/test/scala/endpoints/algebra/EndpointsDocs.scala#response
~~~

Or by using a combinator:

~~~ scala src=../../../../../algebras/algebra/src/test/scala/endpoints/algebra/EndpointsDocs.scala#response-combinator
~~~
