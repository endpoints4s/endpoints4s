package endpoints4s.fetch

import endpoints4s.Hashing

import scala.annotation.nowarn

final class EndpointsSettings(val host: Option[String]) extends Serializable {

  override def toString =
    s"EndpointsSettings($host)"

  @nowarn("cat=unchecked")
  override def equals(other: Any): Boolean =
    other match {
      case that: EndpointsSettings =>
        host == that.host
      case _ => false
    }

  override def hashCode(): Int = Hashing.hash(host)

  private[this] def copy(
      host: Option[String] = host
  ): EndpointsSettings =
    new EndpointsSettings(
      host
    )

  def withHost(host: Option[String]): EndpointsSettings = {
    copy(host = host)
  }
}

object EndpointsSettings {

  def apply(): EndpointsSettings = new EndpointsSettings(None)
}
