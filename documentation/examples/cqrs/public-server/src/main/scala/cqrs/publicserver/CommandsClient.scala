package cqrs.publicserver

import cats.effect.Concurrent
import endpoints4s.http4s.client
import cqrs.commands.CommandsEndpoints
import org.http4s.Uri
import org.http4s.client.{Client => Http4sClient}

class CommandsClient[F[_]: Concurrent](
    authority: Uri.Authority,
    scheme: Uri.Scheme,
    http4sClient: Http4sClient[F]
) extends client.Endpoints(authority, scheme, http4sClient)
    with client.JsonEntitiesFromCodecs
    with CommandsEndpoints
