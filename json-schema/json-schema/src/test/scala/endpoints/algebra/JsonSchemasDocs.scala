package endpoints.algebra

trait JsonSchemasDocs extends JsonSchemas {

  locally {
    //#record-type
    case class Rectangle(width: Double, height: Double)
    //#record-type
    //#record-schema
    implicit val rectangleSchema: JsonSchema[Rectangle] = (
      field[Double]("width", Some("Rectangle width")) zip
      field[Double]("height")
    ).invmap((Rectangle.apply _).tupled)(rect => (rect.width, rect.height))
    //#record-schema
  }

  //#sum-type
  sealed trait Shape
  case class Circle(radius: Double) extends Shape
  case class Rectangle(width: Double, height: Double) extends Shape
  //#sum-type

  object Shape {
    implicit val schema: JsonSchema[Shape] = {

      val circleSchema =
        field[Double]("radius").invmap(Circle)(_.radius)

      val rectangleSchema = (
        field[Double]("width") zip
        field[Double]("height")
      ).invmap((Rectangle.apply _).tupled)(r => (r.width, r.height))

      //#sum-type-schema
      // Given a `circleSchema: Record[Circle]` and a `rectangleSchema: Record[Rectangle]`
      (
        circleSchema.tagged("Circle") orElse
        rectangleSchema.tagged("Rectangle")
      ).invmap[Shape] {
        case Left(circle) => circle
        case Right(rect)  => rect
      } {
        case c: Circle    => Left(c)
        case r: Rectangle => Right(r)
      }
      //#sum-type-schema
    }
  }

  locally {
    //#enum-status
    sealed trait Status
    case object Active extends Status
    case object Inactive extends Status
    case object Obsolete extends Status
    //#enum-status
    //#enum-status-schema
    implicit lazy val statusSchema: JsonSchema[Status] =
      enumeration[Status](Seq(Active, Inactive, Obsolete))(_.toString)
    //#enum-status-schema
  }

}
