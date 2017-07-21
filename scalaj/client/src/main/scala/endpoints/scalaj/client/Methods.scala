package endpoints.scalaj.client

import endpoints.algebra

trait Methods extends algebra.Methods {

   type Method = String

   def Get: Method = "GET"

   def Post: Method = "POST"

   def Put: Method = "PUT"

   def Delete: Method = "DELETE"

}
