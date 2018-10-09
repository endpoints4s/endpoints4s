# Design in a nutshell

The `endpoints.algebra.Endpoints` trait used in the [quick start](quick-start.md) provides members that
bring **vocabulary** to describe HTTP endpoints (e.g. the `endpoint`, `get`, `post`,
etc. methods), but these members are all abstract. Furthermore,
**their types are also abstract**.

For instance, consider the following truncated version of `endpoints.algebra.Endpoints`:

~~~ scala
trait Endpoints {
  def get[A](url: Url[A]): Request[A]

  /** A request that carries an `A` information */
  type Request[A]
  /** An URL that carries an `A` information */
  type Url[A]
}
~~~

The `get` method builds a `Request[A]` out of an `Url[A]`. Let’s put aside the `Url[A]` type and focus on
`Request[A]`.

The `Request[A]` type defines an HTTP request that *carries* an information `A`. From a client point
of view, this `A` is what is **needed** to build a `Request[A]`. From a server point of view, this `A`
is what is **provided** when processing a `Request[A]`.

Let’s see the semantics that is given to `Request[A]` by the `endpoints.xhr.Endpoints` trait:

~~~ scala
trait Endpoints extends algebra.Endpoints {
  type Request[A] = js.Function1[A, XMLHttpRequest]
}
~~~

As previously said, from a client point of view we want to send requests and get responses. So, `Request[A]`
has the semantics of a recipe to build an `XMLHttpRequest` out of an `A` value.

Here is the semantics given by the `endpoints.play.server.Endpoints` trait:

~~~ scala
trait Endpoints extends algebra.Endpoints {
  type Request[A] = RequestHeader => Option[BodyParser[A]]
}
~~~

The aim of the `endpoints.play.server.Endpoints` trait is to provide a Play router for a given set of HTTP endpoints. So,
a `Request[A]` is a function that checks if an incoming request matches this endpoint, and in such
a case it returns a `BodyParser[A]` that pulls an `A` out of the request.

As you can see, each implementation brings its own **concrete semantic type** for `Request[A]`.

According to B. Oliveira [1], we say that `endpoints.algebra.Endpoints` is an *object algebra interface* and that `endpoints.xhr.Endpoints`
and `endpoints.play.server.Endpoints` are *object algebras*. Note that we use an encoding borrowed from C. Hofer [2], which encodes
*carrier types* with type members rather than type parameters.

- [1] B. C. d. S. Oliveira et. al. Extensibility for the Masses, Practical Extensibility with Object Algebras, ECOOP,
2012 ([pdf](http://www.cs.utexas.edu/~wcook/Drafts/2012/ecoop2012.pdf))
- [2] C. Hofer at. al. Polymorphic Embedding of DSLs, GPCE, 2008 ([pdf](http://www.daimi.au.dk/~ko/papers/gpce50_hofer.pdf))
