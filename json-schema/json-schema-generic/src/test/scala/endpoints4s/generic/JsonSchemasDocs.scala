package endpoints4s.generic

import scala.annotation.nowarn

trait JsonSchemasDocs extends JsonSchemas {

  sealed trait Shape
  case class Circle(radius: Double) extends Shape
  case class Rectangle(width: Double, height: Double) extends Shape

  //#generic-schema
  implicit val shapeSchema: JsonSchema[Shape] = genericJsonSchema
  //#generic-schema

  locally {
    //#explicit-schema
    implicit val rectangleSchema: JsonSchema[Rectangle] = (
      field[Double]("width") zip
        field[Double]("height")
    ).as[Rectangle]
    //#explicit-schema
  }: @nowarn("cat=unused-locals")

  locally {
    //#documented-generic-schema
    @discriminator("kind")
    @title("Geometric shape")
    @name("ShapeSchema")
    sealed trait Shape

    @name("CircleSchema")
    case class Circle(radius: Double) extends Shape

    @name("RectangleSchema")
    @docs("A quadrilateral with four right angles")
    case class Rectangle(
        @docs("Rectangle width") width: Double,
        height: Double
    )
    //#documented-generic-schema
  }
}
