package overview

//#relevant-code
import play.core.server.NettyServer

object Main extends App {
  NettyServer.fromRouter()(CounterServer.routes)
}
//#relevant-code