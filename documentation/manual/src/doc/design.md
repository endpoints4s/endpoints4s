# Design in a nutshell

You have seen in the [quick start](quick-start.md) page that using the _endpoints_ library consists in
first defining abstract **descriptions** of HTTP endpoints, and then producing clients, servers, or
documentation, by **interpreting** these descriptions.

Here is an example of endpoint description:

~~~ scala src=../../../../documentation/examples/quickstart/endpoints/src/main/scala/quickstart/CounterEndpoints.scala#get-endpoint-definition
~~~

Endpoint descriptions are defined in terms of operations (e.g., `endpoint`, `get`, `path`, `ok`, etc.)
provided by traits living in the [`endpoints.algebra` package](unchecked:/api/endpoints/algebra/index.html).
These operations are all **abstract**. Furthermore, their return **types** are also abstract. Their
purpose is only to define the **rules** for constructing and combining parts of HTTP endpoint
descriptions. This is why they are called “algebra interfaces”, or just **algebras**.

For instance, consider the following truncated version of the
[`Endpoints` algebra](unchecked:/api/endpoints/algebra/Endpoints.html):

~~~ scala
package endpoints.algebra

trait Endpoints {
  /** A request that uses the method GET and the given URL */
  def get[A](url: Url[A]): Request[A]

  /** A request that carries an `A` information */
  type Request[A]
  /** An URL that carries an `A` information */
  type Url[A]
}
~~~

Here, the `get` method provides a way to define an HTTP request description from a URL description.

> {.note}
> Since the `Request[A]` type is abstract, the _only_ way to construct a value of that type is
> by calling a method that returns a `Request[A]`, such as the method `get`, in the above
> code. For this reason, we say that the method `get` is a **constructor** for `Request[A]`.

The `Request[A]` type models an HTTP request that *carries* an information of type `A`. From a client
point of view, this `A` is what is **needed** to build a `Request[A]`. Conversely, from a server point
of view, this `A` is what is **provided** by an incoming a `Request[A]`.

You have seen in the “quick start” page that **interpreters** give semantics to the algebras. They
do so by fixing their type members and implementing their methods accordingly. For instance, here is the
semantics given to `Request[A]` by the Scala.js client interpreter:

~~~ scala
package endpoints.xhr

trait Endpoints extends endpoints.algebra.Endpoints {
  type Request[A] = js.Function1[A, XMLHttpRequest]
}
~~~

As previously said, from a client point of view we want to send requests and get responses. So, `Request[A]`
has the semantics of a function that builds an `XMLHttpRequest` out of an `A` value.

Here is the semantics given by the Play-based server interpreter:

~~~ scala
package endpoints.play.server

trait Endpoints extends endpoints.algebra.Endpoints {
  type Request[A] = RequestHeader => Option[BodyParser[A]]
}
~~~

The aim of the `endpoints.play.server.Endpoints` trait is to provide a Play router for a given set of HTTP endpoints. So,
a `Request[A]` is a function that checks if an incoming request matches this endpoint, and in such
a case it returns a `BodyParser[A]` that decodes a value of type `A` from the request.

As you can see, each interpreter brings its own **concrete semantic type** for `Request[A]`. Client
interpreters typically fix the `Request[A]` type to a function that *takes* an `A` as parameter. Conversely,
server interpreters typically fix the `Request[A]` type to a function that *returns* an `A`. Can you guess
what documentation interpreters do with this type parameter `A`? You can see the answer
[here](unchecked:/api/endpoints/openapi/Requests.html#Request[A]=Requests.this.DocumentedRequest). It is
discarded because this type `A` models the information that is carried by an actual request, at run-time,
but the documentation is static (so, there is no `A` value to deal with).

> {.note}
> This technique has been described in details by Bruno C. d. S. Oliveira *et al.* [1].
> Note that we use a variant discovered by Christian Hofer *et al.* [2], which uses
> type members rather than type parameters.

## Summary

**Algebras** are traits that provide abstract type members and methods defining how to
**construct** and **combine** endpoint descriptions.

**Interpreters** are traits that extend algebras, and give them a concrete meaning by
fixing their type members and implementing their methods accordingly.

## Next Step

Discover the hierarchy of [algebras](algebras-and-interpreters.md).

---

- [1] B. C. d. S. Oliveira et. al. Extensibility for the Masses, Practical Extensibility with Object Algebras, ECOOP,
2012 ([pdf](https://www.cs.utexas.edu/~wcook/Drafts/2012/ecoop2012.pdf))
- [2] C. Hofer et al. Polymorphic Embedding of DSLs, GPCE, 2008 ([pdf](https://www.informatik.uni-marburg.de/~rendel/hofer08polymorphic.pdf))
