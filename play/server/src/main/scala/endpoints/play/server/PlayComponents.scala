package endpoints.play.server

import play.api.BuiltInComponents
import play.api.http.{FileMimeTypes, HttpErrorHandler}
import play.api.mvc.{DefaultActionBuilder, PlayBodyParsers}

import scala.concurrent.ExecutionContext

/**
  * Play components needed by the interpreter
  */
trait PlayComponents {
  def playBodyParsers: PlayBodyParsers
  def httpErrorHandler: HttpErrorHandler
  def defaultActionBuilder: DefaultActionBuilder
  def fileMimeTypes: FileMimeTypes
  implicit def executionContext: ExecutionContext
}

object PlayComponents {

  /** Creates the `PlayComponents` from a `BuiltInComponents` instance
    *
    * This can be useful in conjunction with the usage of `fromXxxWithComponents`
    * methods of Play framework:
    *
    * {{{
    *   val serverConfig = ServerConfig(...)
    *   NettyServer.fromRouterWithComponents(serverConfig) { builtInComponents =>
    *     val playComponents = PlayComponents.fromBuiltInComponents(builtInComponents)
    *     ...
    *   }
    * }}}
    */
  def fromBuiltInComponents(
      builtInComponents: BuiltInComponents
  ): PlayComponents =
    new PlayComponents {
      def playBodyParsers: PlayBodyParsers = builtInComponents.playBodyParsers
      def httpErrorHandler: HttpErrorHandler =
        builtInComponents.httpErrorHandler
      def defaultActionBuilder: DefaultActionBuilder =
        builtInComponents.defaultActionBuilder
      def fileMimeTypes: FileMimeTypes = builtInComponents.fileMimeTypes
      implicit def executionContext: ExecutionContext =
        builtInComponents.executionContext
    }

}
