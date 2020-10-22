package endpoints4s.generic

/** Adds a description to a case class field, a case class, or a sealed trait.
  *
  * Annotate a case class field, case class, or sealed trait with this
  * annotation to set a description for the schema or the record field.
  *
  * @param text Description of the annotated schema or field
  */
case class docs(text: String) extends scala.annotation.Annotation

/** Defines the title of a generic schema.
  *
  * Annotate a sealed trait or case class definition with this annotation
  * to define its schema title.
  *
  * @param text Title of the schema
  */
case class title(value: String) extends scala.annotation.Annotation

/** Defines the name of a generic schema.
  *
  * Annotate a sealed trait or case class definition with this annotation
  * to define its schema name. Setting the name of a schema explicitly means
  * that you can control exactly what the URI of the JSON schema will be in the
  * OpenAPI documentation.
  *
  * @note The name of the schema is used internally by OpenAPI in the URI that
  *       gets used to refer to the schema. Consequently, the name set here
  *       should include only characters allowed in URIs.
  *
  * @see Use [[title]] to customize the user-friendly name of the schema in the
  *      OpenAPI documentation
  *
  * @param value Name of the schema
  */
case class name(value: String) extends scala.annotation.Annotation

/** Specifies that a generic schema should not have a name.
  *
  * Annotate a sealed trait or case class definition with this annotation to
  * prevent the schema from being named. This is sometimes useful for forcing
  * nested schemas to be inlined in OpenAPI documentation.
  */
case class unnamed() extends scala.annotation.Annotation

/** Defines the name of the discriminator field of a generic tagged schema.
  *
  * Annotate a sealed trait definition with this annotation to define
  * the name of its discriminator field.
  *
  * @param name Name of the tagged discriminator field
  */
case class discriminator(name: String) extends scala.annotation.Annotation
