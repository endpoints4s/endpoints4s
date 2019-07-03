package endpoints.akkahttp.server.playjson

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import endpoints.akkahttp.server.{BasicAuthentication, Endpoints, JsonEntities, JsonEntitiesFromCodec}
import endpoints.algebra
import endpoints.algebra.{JsonFromCodecTestApi, playjson}
import org.scalatest.{Matchers, WordSpec}

import scala.language.reflectiveCalls
import endpoints.akkahttp.server.EndpointsTestApi 

/* implements the endpoint using a codecs-based json handling */
class EndpointsCodecsTestApi extends EndpointsTestApi
  with JsonFromCodecTestApi
  with playjson.JsonFromPlayJsonCodecTestApi
  with JsonEntitiesFromCodec
  with playjson.JsonEntitiesFromCodec
