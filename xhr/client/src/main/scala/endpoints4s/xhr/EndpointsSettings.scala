package endpoints4s.xhr

import endpoints4s.Hashing

import scala.concurrent.duration.{FiniteDuration}

/** Settings for XHR interpreter.
  * @param baseUri Base of the URI of the service that implements the endpoints, can be absolute or relative (e.g. "http://foo.com" or "/bar")
  */
final class EndpointsSettings private (
    val baseUri: Option[String],
    val timeout: Option[FiniteDuration]
) extends Serializable {

  override def toString =
    s"EndpointsSettings($baseUri, $timeout)"

  override def equals(other: Any): Boolean =
    other match {
      case that: EndpointsSettings =>
        baseUri == that.baseUri && timeout == that.timeout
      case _ => false
    }

  override def hashCode(): Int = Hashing.hash(baseUri, timeout)

  private[this] def copy(
      baseUri: Option[String] = baseUri,
      timeout: Option[FiniteDuration] = timeout
  ): EndpointsSettings =
    new EndpointsSettings(
      baseUri,
      timeout
    )

  def withBaseUri(baseUri: Option[String]): EndpointsSettings = {
    copy(baseUri = baseUri)
  }

  def withTimeout(timeout: Option[FiniteDuration]) = {
    copy(timeout = timeout)
  }
}

object EndpointsSettings {

  def apply(): EndpointsSettings = new EndpointsSettings(None, None)
}
