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
      field[Double]("width") :*:
      field[Double]("height")
    ).as[Rectangle]
    //#explicit-schema
  }
}
