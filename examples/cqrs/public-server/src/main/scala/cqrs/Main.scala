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
  implicit val materializer: Materializer = ActorMaterializer()

  try {

    val wsComponents: AhcWSComponents = new AhcWSComponents

    val queriesService = new QueriesService(wsComponents.wsClient)

    import queriesService.{circeJsonEncoder, circeJsonDecoder}

    queriesService.query(FindAll)(circeJsonEncoder(QueryReq.queryEncoder), circeJsonDecoder(QueryResp.queryDecoder))
      .foreach { response =>
        println(s"find all response = $response")
        wsComponents.wsClient.close()
        actorSystem.terminate()
      }

  } catch {
    case t: Throwable =>
      t.printStackTrace()
      actorSystem.terminate()
  }

}

class AhcWSComponents(implicit materializer: Materializer) {
  val applicationLifecycle = new DefaultApplicationLifecycle
  val environment = Environment.simple(mode = Mode.Prod)
  lazy val wsClientConfig: WSClientConfig = WSClientConfig()
  lazy val ahcWsClientConfig: AhcWSClientConfig = AhcWSClientConfig()
  lazy val wsApi: WSAPI = new AhcWSAPI(environment, ahcWsClientConfig, applicationLifecycle)
  lazy val wsClient: WSClient = wsApi.client
}
