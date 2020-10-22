package endpoints4s.algebra

import endpoints4s.Hashing

/** @see [[https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md#tagObject]]
  */
final class Tag private (
    val name: String,
    val description: Option[String],
    val externalDocs: Option[ExternalDocumentationObject]
) extends Serializable {

  override def toString =
    s"Tag($name, $description, $externalDocs)"

  override def equals(other: Any): Boolean =
    other match {
      case that: Tag =>
        name == that.name && description == that.description && externalDocs == that.externalDocs
      case _ => false
    }

  override def hashCode(): Int =
    Hashing.hash(name, description, externalDocs)

  private[this] def copy(
      name: String = name,
      description: Option[String] = description,
      externalDocs: Option[ExternalDocumentationObject] = externalDocs
  ): Tag = new Tag(name, description, externalDocs)

  def withName(name: String): Tag =
    copy(name = name)

  def withDescription(description: Option[String]): Tag =
    copy(description = description)

  def withExternalDocs(externalDocs: Option[ExternalDocumentationObject]): Tag =
    copy(externalDocs = externalDocs)

}

object Tag {

  /** Creates a new Tag without a description and external documentation object.  Use
    * Tag(..).withDescription(..).withExternalDocs(..) to add those attributes.
    */
  def apply(name: String) = new Tag(name, None, None)

}

/** @see [[https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md#externalDocumentationObject]]
  */
final class ExternalDocumentationObject private (
    val url: String,
    val description: Option[String]
) extends Serializable {

  override def toString: String =
    s"ExternalDocumentationObject($url, $description)"

  override def equals(other: Any): Boolean =
    other match {
      case that: ExternalDocumentationObject =>
        url == that.url && description == that.description
      case _ => false
    }

  override def hashCode(): Int =
    Hashing.hash(url, description)

  def withUrl(url: String): ExternalDocumentationObject =
    new ExternalDocumentationObject(url, description)

  def withDescription(
      description: Option[String]
  ): ExternalDocumentationObject =
    new ExternalDocumentationObject(url, description)
}

object ExternalDocumentationObject {

  /** This creates an ExternalDocumentationObject without a description. Use
    * ExternalDocumentationObject(..).withDescription(..) to add a description.
    */
  def apply(url: String) = new ExternalDocumentationObject(url, None)
}
