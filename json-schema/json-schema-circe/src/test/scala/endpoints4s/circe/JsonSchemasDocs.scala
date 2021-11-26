package endpoints4s.circe

import io.circe
import io.circe.Json
import io.circe.syntax._

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

      //#sum-type-schema
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
      //#sum-type-schema
    }
  }

  //#codec
  import JsonSchema._
  val shape: Shape = Circle(42)
  val shapeJson: Json = shape.asJson
  val maybeShape: Either[circe.Error, Shape] = shapeJson.as[Shape]
  //#codec

}
