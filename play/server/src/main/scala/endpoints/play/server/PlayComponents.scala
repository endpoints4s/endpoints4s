package endpoints.play.server

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import play.api.ApplicationLoader.Context
import play.api.{ApplicationLoader, Configuration, DefaultApplication, Environment}
import play.api.http._
import play.api.inject.{ApplicationLifecycle, NewInstanceInjector, SimpleInjector}
import play.api.libs.Files._
import play.api.libs.concurrent.ActorSystemProvider
import play.api.mvc._
import play.api.mvc.request.DefaultRequestFactory
import play.api.routing.Router
import play.api.routing.Router.Routes
import play.core.SourceMapper
import play.core.server.ServerConfig

import scala.concurrent.ExecutionContext

/**
  * Play components needed by the interpreter
  */
trait PlayComponents {
  def environment: Environment
  def configuration: Configuration
  def applicationLifecycle: ApplicationLifecycle
  def actorSystem: ActorSystem
  implicit def executionContext: ExecutionContext
  implicit def materializer: Materializer
  def httpConfiguration: HttpConfiguration
  def httpErrorHandler: HttpErrorHandler
  def playBodyParsers: PlayBodyParsers
  def actionBuilder: ActionBuilder[Request, AnyContent]
  def fileMimeTypes: FileMimeTypes
}

/**
  * Instantiation of the Play components for a given server configuration.
  * @param config Server configuration
  */
class DefaultPlayComponents(config: ServerConfig) extends PlayComponents {
  val environment: Environment = Environment.simple(mode = config.mode)
  val configuration: Configuration = config.configuration
  val context: Context = ApplicationLoader.createContext(environment)
  val applicationLifecycle: ApplicationLifecycle = context.lifecycle
  val sourceMapper: Option[SourceMapper] = context.sourceMapper
  val actorSystem: ActorSystem = new ActorSystemProvider(environment, configuration, applicationLifecycle).get
  implicit val materializer: Materializer = ActorMaterializer()(actorSystem)
  implicit val executionContext: ExecutionContext = actorSystem.dispatcher

  val httpConfiguration: HttpConfiguration = HttpConfiguration.fromConfiguration(configuration, environment)
  val httpErrorHandler: HttpErrorHandler = new DefaultHttpErrorHandler(environment, configuration, sourceMapper, None)
  val fileMimeTypes: FileMimeTypes = new DefaultFileMimeTypesProvider(httpConfiguration.fileMimeTypes).get
  val tempFileReaper: TemporaryFileReaper = new DefaultTemporaryFileReaper(actorSystem, TemporaryFileReaperConfiguration.fromConfiguration(configuration))
  val tempFileCreator: TemporaryFileCreator = new DefaultTemporaryFileCreator(applicationLifecycle, tempFileReaper)
  val playBodyParsers: PlayBodyParsers = PlayBodyParsers(tempFileCreator, httpErrorHandler, httpConfiguration.parser)
  val actionBuilder: DefaultActionBuilder = DefaultActionBuilder(playBodyParsers.default)
}

object HttpServer {
  def apply(config: ServerConfig, playComponents: PlayComponents, routes: Routes): play.core.server.NettyServer = {
    val httpRequestHandler = new DefaultHttpRequestHandler(Router.from(routes), playComponents.httpErrorHandler, playComponents.httpConfiguration)
    val application = new DefaultApplication(
      playComponents.environment,
      playComponents.applicationLifecycle,
      new SimpleInjector(NewInstanceInjector),
      playComponents.configuration,
      new DefaultRequestFactory(playComponents.httpConfiguration),
      httpRequestHandler,
      playComponents.httpErrorHandler,
      playComponents.actorSystem,
      playComponents.materializer
    )
    play.core.server.NettyServer.fromApplication(application, config)
  }
}