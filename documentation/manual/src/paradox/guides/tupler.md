# `Tupler`

This guide explains why we use implicit @scaladoc[`Tupler`](endpoints4s.Tupler) parameters
in the signature of some algebra operations and how it works.

## Motivation

As explained in the @ref[design](../design.md) page, to model a request that carries
an information of type `A`, we use the type `Request[A]`. This type `A` is
important because it represents what is needed by clients to build such
a request and what is received by servers to process such a request. For
instance, a request carrying a user id could be modeled as `Request[Long]`.
Clients would have to supply a `Long` value in order to build such a request,
and servers would decode a `Long` value. Tracking these types is important
because it guarantees that requests and responses are well-formed.

Now, suppose that we define a request that has an URL carrying a user id
(of type `Long`), and an entity carrying an `UpdateUser` value (describing
changes to apply to a user resource). Such a request would have type
`Request[(Long, UpdateUser)]`.

To support the definition of such requests, the algebra provides an
operation that, given an URL and a request entity, returns a request.
A naive definition of such an operation could be the following:

~~~ scala
def request[U, E](url: Url[U], entity: RequestEntity[E]): Request[(U, E)]
~~~

So that, given an `Url[Long]` and a `RequestEntity[UpdateUser]`, it
would return a `Request[(Long, UpdateUser)]`.

However, some requests have no entity. An empty request entity is modeled
by the `emptyEntity` constructor, which has type `RequestEntity[Unit]`.

This means that given the above definition of `request`, defining a
request whose URL carries a `Long` value, but having no entity, would
have type `Request[(Long, Unit)]`. Also, to build such requests
clients would have to supply a `(Long, Unit)` value, and to handle
such requests servers would have to process a `(Long, Unit)` value.
However, these `Unit` values are never meaningful: they can not carry
any useful information.

Things are even worse because in practice requests are formed of an
`Url[U]`, a `RequestEntity[E]` and a `RequestHeaders[H]`. So, the
“naive” return type of a request should be `Request[(U, E, H)]`,
even if several of these type parameters are instantiated to `Unit`.

The goal of the `Tupler` type is to compute more useful tuple types
by discarding the `Unit` parameters. For instance, it produces a
`Request[Long]` instead of a `Request[(Long, Unit)]`. Additionally,
nested tuples are flattened: `Request[((Long, Boolean), String)]`
becomes `Request[(Long, Boolean, String)]`.

## How it works

The `Tupler[A, B]` type takes two type parameters `A` and `B`
and defines an abstract type member `Out`. This `Out` type defines
the “useful” form of tupling `A` and `B`.

@@snip [Tupler.scala](/json-schema/json-schema/src/main/scala/endpoints4s/Tupler.scala) { #definition }

Algebra operations that want to tuple types `A` and `B` take as
parameter an implicit `tupler: Tupler[A, B]` and return a
`tupler.Out` instead of an `(A, B)`.

Several implicit instances of `Tupler` are provided. For example,
the following instance returns a pair of `(A, B)` for all types
`A` and `B`:

~~~ scala
implicit def pair[A, B]: Tupler[A, B] { type Out = (A, B) }
~~~

But we have seen that our goal is to special case tupling `Unit`
types. This is achieved by defining another `Tupler` instance
with a higher priority than this one, for the case where one
of the type parameters is `Unit`. For instance:

~~~ scala
implicit def keepFirst[A]: Tupler[A, Unit] { type Out = A }
~~~

The `keepFirst` instance computes the type of tupling `A`
and `Unit`, for all type `A`, to be `A`.

When the operation that takes an implicit `Tupler` as parameter
is called, appropriate instances of `Tupler` will compute the type
of the resulting tuple, by discarding `Unit` types and flattening
nested tuples.
