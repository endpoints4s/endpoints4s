package endpoints.akkahttp.server

import akka.http.scaladsl.model.headers.{Authorization, BasicHttpCredentials, HttpChallenges}
import akka.http.scaladsl.model.{HttpHeader, HttpResponse, StatusCodes=>AkkaStatusCodes}
import akka.http.scaladsl.server.{Directive, Directives}
import endpoints.algebra
import endpoints.algebra.BasicAuthentication.Credentials

/**
  * @group interpreters
  */
trait BasicAuthentication extends algebra.BasicAuthentication with Endpoints {

  /**
    * Extracts the credentials from the request headers.
    * In case of absence of credentials rejects request
    */
  private[endpoints] lazy val basicAuthenticationHeader: RequestHeaders[Credentials] =
  Directives.optionalHeaderValue(extractCredentials).flatMap {
    case Some(credentials) => Directives.pass.tmap(_ => credentials)
    case None => Directive[Tuple1[Credentials]] { _ => //inner is ignored
      import akka.http.scaladsl.model.headers
      Directives.complete(HttpResponse(
        AkkaStatusCodes.Unauthorized,
        scala.collection.immutable.Seq[HttpHeader](headers.`WWW-Authenticate`(HttpChallenges.basic("Realm")))
      ))
    }
  }

  private def extractCredentials: HttpHeader => Option[Credentials] = {
    case Authorization(BasicHttpCredentials(username, password)) => Some(Credentials(username, password))
    case _ => None
  }

}
