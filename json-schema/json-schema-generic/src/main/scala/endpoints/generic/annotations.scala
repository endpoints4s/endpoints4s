package endpoints.generic

/**
  * Documents a case class field, case class, or sealed trait.
  *
  * Annotate a case class field, case class, or selaed trait with this
  * annotation to define its documentation.
  *
  * @param text Description of the annotated field or schema
  */
case class docs(text: String) extends scala.annotation.Annotation

/**
  * Defines the title of a generic schema.
  *
  * Annotate a sealed trait or case class definition with this annotation
  * to define its schema title.
  *
  * @param text Title of the schema
  */
case class title(value: String) extends scala.annotation.Annotation

/**
  * Defines the name of a generic schema.
  *
  * Annotate a sealed trait or case class definition with this annotation
  * to define its schema name.
  *
  * @param value Name of the schema
  */
case class name(value: String) extends scala.annotation.Annotation

/**
  * Specifies that a generic schema should not have a name.
  *
  * Annotate a sealed trait or case class definition with this annotation to
  * prevent the schema from being named. This is sometimes useful for forcing
  * nested schemas to be inlined in OpenAPI documentation.
  */
case class unnamed() extends scala.annotation.Annotation

/**
  * Defines the name of the discriminator field of a generic tagged schema.
  *
  * Annotate a sealed trait definition with this annotation to define
  * the name of its discriminator field.
  *
  * @param name Name of the tagged discriminator field
  */
case class discriminator(name: String) extends scala.annotation.Annotation
