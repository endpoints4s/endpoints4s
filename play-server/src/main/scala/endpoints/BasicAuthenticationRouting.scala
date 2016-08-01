package endpoints

import java.util.Base64

import play.api.http.HeaderNames.AUTHORIZATION
import play.api.libs.streams.Accumulator
import play.api.mvc.{BodyParser, Call, RequestHeader}

trait BasicAuthenticationRouting extends BasicAuthenticationAlg with PlayRouting {

  def authenticatedGet[A](url: Url[A])(implicit tupler: Tupler[A, Credentials]): Request[tupler.Out] =
    new Request[tupler.Out] {
      def decode(requestHeader: RequestHeader) =
        if (requestHeader.method == "GET") {
          val maybeCredentials =
            requestHeader.headers.get(AUTHORIZATION)
              .filter(h => h.startsWith("Basic ")) // FIXME case sensitivity?
              .flatMap { h =>
              val userPassword =
                new String(Base64.getDecoder.decode(h.drop(6)))
              val i = userPassword.indexOf(':')
              if (i < 0) None
              else {
                val (user, password) = userPassword.splitAt(i)
                Some(Credentials(user, password.drop(1)))
              }
            }
          maybeCredentials
            .flatMap { credentials =>
              url.decodeUrl(requestHeader)
                .map(a => BodyParser(_ => Accumulator.done(Right(tupler(a, credentials)))))
            }
        } else None // TODO return an Unauthorized result

      def encode(out: tupler.Out) = Call("GET", url.encodeUrl(tupler.unapply(out)._1))
    }
}
