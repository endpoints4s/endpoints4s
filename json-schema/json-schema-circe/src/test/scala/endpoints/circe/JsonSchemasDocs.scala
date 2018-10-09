package endpoints.circe

import endpoints.algebra
import io.circe
import io.circe.Json
import io.circe.syntax._

trait JsonSchemasDocs extends algebra.JsonSchemasDocs with JsonSchemas {

  //#codec
  import JsonSchema._
  val shape: Shape = Circle(42)
  val shapeJson: Json = shape.asJson
  val maybeShape: Either[circe.Error, Shape] = shapeJson.as[Shape]
  //#codec

}
