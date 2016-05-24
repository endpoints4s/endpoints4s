package sample

import endpoints.{CirceCodecsClient, XhrClient}

object Api extends ApiAlg with XhrClient with CirceCodecsClient with AssetsClient