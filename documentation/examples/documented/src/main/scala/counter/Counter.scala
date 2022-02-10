// This example shows how to define a web service and its documentation using ''endpoints''.
// We start by defining a description of the HTTP API in Scala and then we derive
// the server implementation and the OpenAPI documentation from this description.
// The application implements a counter whose value can be queried and updated
// by applying an operation to it.

package counter

import cats.effect.Sync
import org.http4s.Headers

import java.util.concurrent.atomic.AtomicInteger

// Our domain model just contains a counter value
case class Counter(value: Int)

// The operations that we can apply to our counter
sealed trait Operation
object Operation {
  // Reset the counter value to the given `value`
  case class Set(value: Int) extends Operation
  // Add `delta` to the counter value
  case class Add(delta: Int) extends Operation
}

// Description of the HTTP API
//#documented-endpoints
import endpoints4s.{algebra, generic}

trait CounterEndpoints
    extends algebra.Endpoints
    with algebra.JsonEntitiesFromSchemas
    with generic.JsonSchemas {

  // HTTP endpoint for querying the current value of the counter. Uses the HTTP
  // verb ''GET'' and the path ''/counter''. Returns the current value of the counter
  // in a JSON object. (see below for the `counterJson` definition)
  val currentValue: Endpoint[Unit, Counter] =
    endpoint(get(path / "counter"), counterJsonResponse)

  // HTTP endpoint for updating the value of the counter. Uses the HTTP verb ''POST''
  // and the path ''/counter''. The request entity contains an `Operation` object encoded
  // in JSON. The endpoint returns the current value of the counter in a JSON object.
  val update: Endpoint[Operation, Counter] = endpoint(
    post(
      path / "counter",
      jsonRequest[Operation],
      docs = Some("The operation to apply to the counter")
    ),
    counterJsonResponse
  )

  // Since both the `currentValue` and `update` endpoints return the same
  // information, we define it once and just reuse it. Here, we say
  // that they return an HTTP response whose entity contains a JSON document
  // with the counter value
  lazy val counterJsonResponse: Response[Counter] =
    ok(jsonResponse[Counter], docs = Some("The counter current value"))

  // We generically derive a data type schema. This schema
  // describes that the case class `Counter` has one field
  // of type `Int` named “value”
  implicit lazy val jsonSchemaCounter: JsonSchema[Counter] = genericJsonSchema

  // Again, we generically derive a schema for the `Operation`
  // data type. This schema describes that `Operation` can be
  // either `Set` or `Add`, and that `Set` has one `Int` field
  // name `value`, and `Add` has one `Int` field named `delta`
  implicit lazy val jsonSchemaOperation: JsonSchema[Operation] =
    genericJsonSchema

}
//#documented-endpoints

// OpenAPI documentation for the HTTP API described in `CounterEndpoints`
import endpoints4s.openapi
import endpoints4s.openapi.model.{Info, OpenApi}

object CounterDocumentation
    extends CounterEndpoints
    with openapi.Endpoints
    with openapi.JsonEntitiesFromSchemas {

  val api: OpenApi =
    openApi(
      Info(title = "API to manipulate a counter", version = "1.0.0")
    )(currentValue, update)
}

// Implementation of the HTTP API and its business logic
import cats.effect.IO
import endpoints4s.http4s.server
import org.http4s.HttpRoutes

object CounterServer
    extends server.Endpoints[IO]
    with server.JsonEntitiesFromSchemas
    with CounterEndpoints { parent =>

  // Internal state of our counter
  private val value = new AtomicInteger(0)

  // We map each endpoint to its business logic and get a Play router from them
  // Note that the business logic is really just the ''business logic'': there is
  // nothing about HTTP requests, responses or JSON here. All the HTTP related
  // aspects were defined earlier in the `CounterEndpoints` trait.
  // As a consequence, our `delegate` server implementation manages the request
  // decoding and response encoding for us, so that here we can just use our
  // business domain data types
  val routes = routesFromEndpoints(
    currentValue.implementedBy(_ => Counter(value.get())),
    update.implementedBy {
      case Operation.Set(newValue) =>
        value.set(newValue)
        Counter(newValue)
      case Operation.Add(delta) =>
        val newValue = value.addAndGet(delta)
        Counter(newValue)
    }
  )
}

import cats.effect.{ExitCode, IOApp}
import org.http4s.blaze.server._
import org.http4s.dsl.io._
import org.http4s.headers.Location
import org.http4s.implicits._
import org.http4s.Response

//#main-only
object Main extends IOApp {
  // JVM entry point that starts the HTTP server
  override def run(args: List[String]): IO[ExitCode] = {
    BlazeServerBuilder[IO]
      .bindHttp(sys.props.get("http.port").map(_.toInt).getOrElse(8080))
      .withHttpApp(
        HttpRoutes.of(CounterServer.routes.orElse(DocumentationServer.routes)).orNotFound
      )
      .resource
      .use(_ => IO.never)
      .as(ExitCode.Success)
  }

  //#main-only
  private val redirectToIndex: Response[IO] =
    Response(TemporaryRedirect, headers = Headers(Location(uri"/assets/index.html")))

  object DocumentationServer
      extends server.Endpoints[IO]
      with server.JsonEntitiesFromEncodersAndDecoders
      with server.Assets {

    def EffectSync: Sync[Effect] = Sync[IO]

    // HTTP endpoint serving documentation. Uses the HTTP verb ''GET'' and the path
    // ''/documentation.json''. Returns an OpenAPI document.
    val documentation = endpoint[Unit, OpenApi](
      get(path / "documentation.json"),
      ok(jsonResponse[OpenApi])
    )

    // We “render” the OpenAPI document using the swagger-ui, provided as static assets
    val assets = assetsEndpoint(path / "assets" / assetSegments())

    // Redirect the root URL “/” to the “index.html” asset for convenience
    val root: Endpoint[Unit, Unit] = endpoint(get(path), _ => redirectToIndex)

    val routes = routesFromEndpoints(
      documentation.implementedBy(_ => CounterDocumentation.api),
      assets.implementedBy(assetsResources(pathPrefix = Some("/public"))),
      root.implementedBy(_ => ())
    )

  }
  //#main-only
}
//#main-only
