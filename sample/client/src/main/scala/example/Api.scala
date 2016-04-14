package example

import endpoints.{CirceCodecsClient, XhrClient}

object Api extends ApiAlg with XhrClient with CirceCodecsClient
