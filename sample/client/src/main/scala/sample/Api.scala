package sample

import endpoints.{AssetsClient, CirceCodecsClient, ThenableClient, XhrClient}

object Api extends ApiAlg with XhrClient with CirceCodecsClient with AssetsClient with ThenableClient