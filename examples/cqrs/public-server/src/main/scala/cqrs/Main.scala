package cqrs

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import play.api.{Configuration, Environment, Mode}
import play.api.inject.{ApplicationLifecycle, DefaultApplicationLifecycle}
import play.api.libs.ws.{WSAPI, WSClient, WSClientConfig, WSConfigParser}
import play.api.libs.ws.ahc.{AhcWSAPI, AhcWSClientConfig, AhcWSClientConfigParser}

import scala.concurrent.ExecutionContext.Implicits.global

object Main extends App {

  implicit val actorSystem: ActorSystem = ActorSystem("public-server")

  try {

    val wsComponents =
      new AhcWSComponents {
        val applicationLifecycle = new DefaultApplicationLifecycle
        val environment = Environment.simple(mode = Mode.Prod)
        val materializer = ActorMaterializer()
      }

    val queriesService = new QueriesService(wsComponents.wsClient)

    import queriesService.{circeJsonEncoder, circeJsonDecoder}

    queriesService.query(FindAll)(circeJsonEncoder(Query.queryEncoder), circeJsonDecoder(QueryResult.queryDecoder))
      .foreach { response =>
        response: ResourceList
        println(s"find all response = $response")
        wsComponents.applicationLifecycle.stop()
        actorSystem.terminate()
      }

  } catch {
    case t: Throwable =>
      t.printStackTrace()
      actorSystem.terminate()
  }

}

trait AhcWSComponents {

  def environment: Environment

  def applicationLifecycle: ApplicationLifecycle

  def materializer: Materializer

  lazy val wsClientConfig: WSClientConfig = WSClientConfig()
  lazy val ahcWsClientConfig: AhcWSClientConfig = AhcWSClientConfig()
  lazy val wsApi: WSAPI = new AhcWSAPI(environment, ahcWsClientConfig, applicationLifecycle)(materializer)
  lazy val wsClient: WSClient = wsApi.client
}
