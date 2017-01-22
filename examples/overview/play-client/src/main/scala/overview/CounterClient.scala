package overview

//#relevant-code
import endpoints.play.client
import play.api.libs.ws.WSClient
import scala.concurrent.ExecutionContext

class CounterClient(wsClient: WSClient)(implicit ec: ExecutionContext)
  extends client.Endpoints("http://my-counter.com", wsClient)
    with CounterEndpoints
    with client.CirceEntities
//#relevant-code