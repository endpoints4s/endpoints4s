package endpoints4s.xhr

import endpoints4s.Hashing

import scala.annotation.nowarn

final class EndpointsSettings private (val baseUri: Option[String]) extends Serializable {

  override def toString =
    s"EndpointsSettings($baseUri)"

  override def equals(other: Any): Boolean =
    other match {
      case that: EndpointsSettings =>
        baseUri == that.baseUri
      case _ => false
    }

  override def hashCode(): Int = Hashing.hash(baseUri)

  @nowarn("cat=unused")
  private[this] def copy(
      baseUri: Option[String] = baseUri
  ): EndpointsSettings =
    new EndpointsSettings(
      baseUri
    )

  def withBaseUri(baseUri: Option[String]): EndpointsSettings = {
    copy(baseUri = baseUri)
  }
}

object EndpointsSettings {

  def apply(): EndpointsSettings = new EndpointsSettings(None)
}
