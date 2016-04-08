# endpoints

An embedded DSL for defining HTTP endpoints in Scala.

## Canonical example

### Description of the HTTP endpoints

Let’s define a first artifact, cross-compiled for Scala.js, and containing a description of the
endpoints of a Web service.

~~~ scala
import julienrf.endpoints.Endpoints
import io.circe.generic.JsonCodec
/**
  * Defines the HTTP endpoints description of a Web service implementing a counter.
  * This Web service has two endpoints: one for getting the current value of the counter,
  * and one for incrementing it.
  */
trait CounterAlg extends Endpoints {

  /**
    * Get the counter current value.
    * Uses the HTTP verb “GET” and URL path “/current-value”.
    * The response entity is a JSON document representing the counter value.
    */
  val currentValue = endpoint(get(path / "current-value"), response.json[Counter])

  /**
    * Increments the counter value.
    * Uses the HTTP verb “POST” and URL path “/increment”.
    * The request entity is a JSON document representing the increment to apply to the counter.
    * The response entity is empty.
    */
  val increment = endpoint(post(path / "increment", request.json[Increment]), response.empty)

}

@JsonCodec
case class Counter(value: Int)

@JsonCodec
case class Increment(step: Int)
~~~

### JavaScript (Scala.js) client

The following code is located in a Scala.js-only module, which depends on the first one.

~~~ scala
import julienrf.endpoints.XhrClient
/**
  * Defines an HTTP client for the endpoints described in the `CounterAlg` trait.
  * The derived HTTP client uses XMLHttpRequest to perform requests.
  */
object Counter extends CounterAlg with XhrClient
~~~

And then:

~~~ scala
import scala.scalajs.js
/**
  * Performs an XMLHttpRequest on the `currentValue` endpoint, and then deserializes the JSON
  * response as a `Counter`.
  */
val eventuallyCounter: js.Promise[Counter] = Counter.currentValue()
~~~

And also:

~~~ scala
/**
  * Serializes the `Increment` value into JSON and performs an XMLHttpRequest on the `increment` endpoint.
  */
val eventuallyDone: js.Promise[Unit] = Counter.increment(Increment(42))
~~~

### Service implementation (backed by Play framework)

The following code is located in a JVM-only module, which depends on the first one.

~~~ scala
import julienrf.endpoints.PlayRouting
import scala.concurrent.stm.Ref

/**
  * Defines a Play router (and reverse router) for the endpoints described in the `CounterAlg` trait.
  */
object Counter extends CounterAlg with PlayRouting {

  /** Dummy implementation of an in-memory counter */
  val counter = Ref(0)

  val routes = routesFromEndpoints(

    /** Implements the `currentValue` endpoint */
    currentValue.implementedBy(_ => counter.single.get),

    /** Implements the `increment` endpoint */
    increment.implementedBy(inc => counter.single += inc.step))

  )

}
~~~

The `Counter.routes` value is just a `PartialFunction[play.api.mvc.RequestHeader, play.api.mvc.Handler]`.
To get an executable Web server we need to setup a “main” like the following:

~~~ scala
import play.core.server.NettyServer

object Main extends App {
  NettyServer.fromRouter()(Counter.routes)
}
~~~

### What else?

You can also get a Scala/JVM client (which uses `play-ws` under the hood) as follows:

~~~ scala
import julenrf.endpoints.PlayWsClient
import play.api.libs.ws.WSClient
import scala.concurrent.ExecutionContext

class Counter(wsClient: WSClient)(implicit ec: ExecutionContext) extends PlayWsClient(wsClient) with CounterAlg
~~~

Thus, you can distribute a (fully working) JVM client, which is independent of your implementation.

## How it works?

The `Endpoints` trait used in the first module provides members that bring *vocabulary* to describe HTTP
endpoints (e.g. the `endpoint`, `get`, `post`, etc. methods), but these members are all abstract. Furthermore,
their return type is also abstract. A simplified version of the `endpoint` member could be the following:

~~~ scala
trait Endpoints {
  type Endpoint[A, B]
  type Request[A]
  type Response[A]
  def endpoint[A, B](request: Request[A], response: Response[B]): Endpoint[A, B]
}
~~~

Then, traits `XhrClient`, `PlayRouting`, etc. implement these members. Let’s see a simplified implementation
of the `XhrClient` trait:

~~~ scala
trait XhrClient extends Endpoints {
  type Endpoint[A, B] = js.Function1[A, js.Promise[B]]
  type Request[A] = js.Function1[A, XMLHttpRequest]
  type Response[A] = js.Function1[XMLHttpRequest, Option[B]]
  def endpoint[A, B](request: Request[A], response: Response[B]) =
    (a: A) => new Promise((resolve, error) => {
      val xhr = request(a)
      xhr.onload = _ => response(xhr).fold(error("Oops"), b => resolve(b))
      xhr.send()
    }
})
~~~

And here is a simplified implementation of the `PlayRouting` trait:

~~~ scala
trait PlayRouting extends Endpoints {
  type Endpoint[A, B] = (A => B) => (RequestHeader => Option[Result])
  type Request[A] = RequestHeader => Option[A]
  type Response[A] = A => Result
  def endpoint[A, B](request: Request[A], response: Response[B]) =
    implementation => requestHeader => request(requestHeader).map(implementation andThen response)
}
~~~

As you can see, each implementation chooses a different concrete own semantic type for `Endpoint[A, B]`.

According to B. Oliveira [1], we say that `Endpoints` is an *object algebra interface* and that `XhrClient`
and `PlayRouting` are *object algebras*. Note that we use an encoding borrowed from C. Hofer [2], which encodes
*carrier types* with type members rather than type parameters.

- [1] B. C. d. S. Oliveira et. al. Extensibility for the Masses, Practical Extensibility with Object Algebras, ECOOP,
2012 ([pdf](http://www.cs.utexas.edu/~wcook/Drafts/2012/ecoop2012.pdf))
- [2] C. Hofer at. al. Polymorphic Embedding of DSLs, GPCE, 2008 ([pdf](http://www.daimi.au.dk/~ko/papers/gpce50_hofer.pdf))

## TODO

- Pure JavaScript client ;
- Cacheable assets ;
- Server-Sent Events and Web-Sockets endpoints (seens by clients as reactive streams).

## Related works

### Autowire

With autowire, all the communications go through a single HTTP endpoint and use the single marshalling format.
This has the following consequences :

- You can not define RESTful APIs ;
- You can not control response caching ;
- You can not serve assets.

Another difference is that `endpoints` uses no macros at all.

### Thrift? Swagger?

## License

This content is released under the [MIT License](http://opensource.org/licenses/mit-license.php).