package endpoints.play

import io.circe.Json
import play.api.http.{ContentTypes, Writeable}
import play.api.mvc.Codec

object PlayCirce {

  implicit def circeJsonWriteable(implicit codec: Codec): Writeable[Json] =
    new Writeable[Json](json => codec.encode(json.noSpaces), Some(ContentTypes.JSON))

}
