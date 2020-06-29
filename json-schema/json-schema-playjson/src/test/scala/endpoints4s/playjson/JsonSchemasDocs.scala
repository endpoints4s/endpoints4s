package endpoints4s.playjson

import endpoints4s.algebra
import play.api.libs.json.{JsResult, JsValue, Json}

trait JsonSchemasDocs extends algebra.JsonSchemasDocs with JsonSchemas {

  //#codec
  import JsonSchema._
  val shape: Shape = Circle(42)
  val shapeJson: JsValue = Json.toJson(shape)
  val maybeShape: JsResult[Shape] = Json.fromJson[Shape](shapeJson)
  //#codec

}
