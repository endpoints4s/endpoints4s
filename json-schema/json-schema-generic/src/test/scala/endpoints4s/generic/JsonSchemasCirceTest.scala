package endpoints4s
package generic

import io.circe.Json
import org.scalatest.freespec.AnyFreeSpec

class JsonSchemasCirceTest extends AnyFreeSpec {

  object GenecirCodec extends JsonSchemasDocs with JsonSchemas with circe.JsonSchemas

  "sealed trait" in {

    import GenecirCodec._

    val circle = Circle(42)
    val circleJson = Json.obj(
      "type" -> Json.fromString("Circle"),
      "radius" -> Json.fromInt(42)
    )
    val rectangle = Rectangle(42, 43)
    val rectangleJson = Json.obj(
      "type" -> Json.fromString("Rectangle"),
      "width" -> Json.fromInt(42),
      "height" -> Json.fromInt(43)
    )

    assert(shapeSchema.encoder(circle) == circleJson)
    assert(shapeSchema.decoder.decodeJson(circleJson).contains(circle))
    assert(shapeSchema.encoder(rectangle) == rectangleJson)
    assert(shapeSchema.decoder.decodeJson(rectangleJson).contains(rectangle))
  }
}
