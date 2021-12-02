package endpoints4s.playjson

import play.api.libs.json.{JsResult, JsValue, Json}

trait JsonSchemasDocs extends JsonSchemas {

  //#sum-type
  sealed trait Shape
  case class Circle(radius: Double) extends Shape
  case class Rectangle(width: Double, height: Double) extends Shape
  //#sum-type

  object Shape {
    implicit val schema: JsonSchema[Shape] = {

      val circleSchema =
        field[Double]("radius").xmap(Circle(_))(_.radius)

      val rectangleSchema = (
        field[Double]("width") zip
          field[Double]("height")
      ).xmap((Rectangle.apply _).tupled)(r => (r.width, r.height))

      // Given a `circleSchema: Record[Circle]` and a `rectangleSchema: Record[Rectangle]`
      (
        circleSchema.tagged("Circle") orElse
          rectangleSchema.tagged("Rectangle")
      ).xmap[Shape] {
        case Left(circle) => circle
        case Right(rect)  => rect
      } {
        case c: Circle    => Left(c)
        case r: Rectangle => Right(r)
      }
    }
  }

  //#codec
  import JsonSchema._
  val shape: Shape = Circle(42)
  val shapeJson: JsValue = Json.toJson(shape)
  val maybeShape: JsResult[Shape] = Json.fromJson[Shape](shapeJson)
  //#codec

}
