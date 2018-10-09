package endpoints.akkahttp.server

import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.unmarshalling.FromRequestUnmarshaller

import endpoints._

/**
  * Interpreter for [[algebra.JsonEntities]]
  *
  * To use it mix in support for your favourite Json library
  * You can use one of [[https://github.com/hseeberger/akka-http-json hseeberger/akka-http-json]] modules
  *
  * @group interpreters
  */
trait JsonEntities extends algebra.JsonEntities with Endpoints {

  type JsonRequest[A] = FromRequestUnmarshaller[A]

  def jsonRequest[A : JsonRequest]: RequestEntity[A] = Directives.entity[A](implicitly)


  type JsonResponse[A] = ToResponseMarshaller[A]

  def jsonResponse[A : JsonResponse]: Response[A] = entity => Directives.complete(entity)

}
