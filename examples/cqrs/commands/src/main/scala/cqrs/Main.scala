package cqrs

import play.core.server.NettyServer

object Main extends App {

  NettyServer.fromRouter()(Commands.routes)

}
