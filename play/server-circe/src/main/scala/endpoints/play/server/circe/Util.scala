package endpoints.play.server.circe

import io.circe.Json
import play.api.http.{ContentTypes, Writeable}
import play.api.mvc.Codec

object Util {

  implicit def circeJsonWriteable(implicit codec: Codec): Writeable[Json] =
    new Writeable[Json](json => codec.encode(json.noSpaces), Some(ContentTypes.JSON))


}
