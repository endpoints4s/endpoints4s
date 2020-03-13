package endpoints.generic

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
  }

  locally {
    //#documented-generic-schema
    @discriminator("kind")
    @name("ShapeSchema")
    sealed trait Shape

    @name("CircleSchema")
    case class Circle(radius: Double) extends Shape

    @name("RectangleSchema")
    case class Rectangle(
        @docs("Rectangle width") width: Double,
        height: Double
    )
    //#documented-generic-schema
  }
}
