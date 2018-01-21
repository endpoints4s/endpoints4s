package overview

//#relevant-code
import endpoints.play.server.{DefaultPlayComponents, HttpServer}
import play.core.server.ServerConfig

object Main extends App {
  val config = ServerConfig()
  val playComponents = new DefaultPlayComponents(config)
  HttpServer(config, playComponents, new CounterServer(playComponents).routes)
}
//#relevant-code