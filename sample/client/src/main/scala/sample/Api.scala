package sample

import endpoints._

object Api extends ApiAlg with XhrClient
  with CirceCodecsClient with AssetsClient with ThenableClient with OptionalResponseClient