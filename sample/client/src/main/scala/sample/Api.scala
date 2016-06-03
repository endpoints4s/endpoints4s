package sample

import endpoints.{AssetsClient, CirceCodecsClient, XhrClient}

object Api extends ApiAlg with XhrClient with CirceCodecsClient with AssetsClient