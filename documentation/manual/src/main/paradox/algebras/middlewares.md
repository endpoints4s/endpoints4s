# Middlewares

The @ref[`Endpoints`](endpoints.md) algebra provides operations for transforming
endpoint descriptions to enrich their request, their response, or their
documentation. These transformation operations are documented here.

@coordinates[algebra]

@scaladoc[API documentation](endpoints4s.algebra.Endpoints)

## Overview

Middlewares allow you to define reusable transformations of endpoint requests,
responses, or documentation. These transformations are reusable because they
can be applied to arbitrary endpoint descriptions. Let’s look at an example.

Consider a service that requires all the requests to provide a custom header
named `X-API-Key`. The endpoints of such a service could be defined as follows:

@@snip [EndpointsDocs.scala](/algebras/algebra/src/test/scala/endpoints4s/algebra/EndpointsDocs.scala) { #without-middlewares }

We first define the description of the request header in a value
`xApiKeyHeader`, which we add to every endpoint definition. It is nice to be
able to just reuse the `xApiKeyHeader` value everywhere, but if at some
point we wanted to change the way the service supports authentication --- e.g.,
by using a query parameter instead of a header, we would have to update all the
request definitions accordingly.

Alternatively, enriching a request definition with the fact that it also carries
an API key could be defined as a reusable transformation:

@@snip [EndpointsDocs.scala](/algebras/algebra/src/test/scala/endpoints4s/algebra/EndpointsDocs.scala) { #with-middlewares }

The method `withApiKey` takes a request description and enriches it with the
desired header by using the operation `addHeaders`. We apply it to the
subsequent request definitions. Now, if we decided to use a query parameter
to carry the API key instead of a request header, we would just change the
body of the method `withApiKey`:

@@snip [EndpointsDocs.scala](/algebras/algebra/src/test/scala/endpoints4s/algebra/EndpointsDocs.scala) { #api-key-query }

The following sections describe all the transformation operations that can be
applied to requests, responses, and documentation.

@@@ note
As shown, `withApiKey` is a method that takes a `Request[A]` and returns a
`Request[(A, String)]` (the API key is modeled as a `String`). A drawback
of this signature is that if we apply it to a `Request[Unit]` (ie, a request
that carries no information), we get back a `Request[(Unit, String)]`,
although a `Request[String]` would have been preferable (a tuple with an
element of type `Unit` is useless). It is possible to fix this issue by
using an implicit `Tupler`. See the corresponding
@ref[documentation](../guides/tupler.md).
@@@

## Transforming Requests

A request description can be enriched by adding headers or query parameters
with the operations `addHeaders` and `addQueryString`, respectively. Both
operations were showcased in the previous section.

As their names suggest, these operations can only add new headers or query
parameters to a request, they can’t replace the existing constituents of a
request.

## Transforming Responses

Responses can be enriched by using the `orElse` operation (already
documented @ref[here](endpoints.md)), or by using the `addHeaders`
operation, which is similar to the one defined on requests.

Like with requests, these operations can only enrich responses, they
can’t replace their constituents.

## Transforming Endpoints

Complex transformations can be applied to a whole endpoint description
by using the operations `mapRequest`, `mapResponse`, or `mapDocs`, as
shown in the following example, which transforms an arbitrary endpoint
description into an endpoint that is protected by HTTP basic
authentication.

@@snip [EndpointsDocs.scala](/algebras/algebra/src/test/scala/endpoints4s/algebra/EndpointsDocs.scala) { #with-authentication-definition }

The method `withAuthentication` takes any endpoint description, and
enriches its request description to include the `Authorization` header,
and its response description to include the `Unauthorized` response
containing the `WWW-Authenticate` header.

Here is an example of usage of the method `withAuthentication`:

@@snip [EndpointsDocs.scala](/algebras/algebra/src/test/scala/endpoints4s/algebra/EndpointsDocs.scala) { #with-authentication-usage }

The endpoint `unauthenticatedEndpoint` has type `Endpoint[Int, String]`.
Its request carries a value of type `Int` (passed as a query parameter),
and its response carries a value of type `String` (provided by the
response entity).

When we apply the method `withAuthentication` to the endpoint description,
we get back an `Endpoint[(Int, String), Either[String, String]]`.
It carries a pair of type `(Int, String)` in its request (the initial
query parameter of type `Int`, and the credentials content of type
`String`), and now its response has type `Either[String, String]` (it will
be a `Left` with the content of the `WWW-Authenticate` header in case of
incorrect credentials, or a `Right` if authentication succeeds).

The method `mapRequest` on the type `Endpoint[A, B]` takes as parameter
a transformation function. This function turns the initial `Request[A]`
into a `Request[C]` (for any type `C`). So, the resulting endpoint has
type `Endpoint[C, B]`:

@@snip [EndpointsDocs.scala](/algebras/algebra/src/main/scala/endpoints4s/algebra/Endpoints.scala) { #map-request-signature }

The method `mapResponse` works similarly, but it takes as parameter a
transformation function that turns the initial `Response[B]` into a
`Response[C]` (for any type `C`).

Last, there is also a method `mapDocs`, which takes as parameter a
function that transforms the initial `EndpointDocs` into another
`EndpointDocs` value.
