package endpoints.algebra

import endpoints.Hashing

class Tag private (
    val name: String,
    val description: Option[String],
    val externalDocs: Option[ExternalDocumentationObject]
) {

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

  def apply(
      name: String
  ) = new Tag(name, None, None)

}

class ExternalDocumentationObject private (
    val url: String,
    val description: Option[String]
) {

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

  def apply(
      url: String
  ) = new ExternalDocumentationObject(url, None)
}
