package sample

import endpoints._

object Api extends ApiAlg with EndpointXhrClient with CirceCodecXhrClient with AssetXhrClient
  with XhrClientThenable with OptionalResponseXhrClient with BasicAuthenticationXhrClient